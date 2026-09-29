/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package datasource;

import java.util.HashMap;
import java.util.Map;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.InvalidTransactionException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAResource;

/**
 * Adapter that presents a Jakarta transaction manager as a javax.transaction one. Commons DBCP 2
 * (managed data sources) is still bound to the javax.transaction API, while the transaction
 * managers in a Jakarta EE environment implement jakarta.transaction. The javax.transaction API is
 * private to this bundle.
 * <p>
 * The status constants of both APIs have identical values, so they are passed as is.
 */
final class JtaAdapter {
    private JtaAdapter() {
    }

    static TransactionManager adapt(jakarta.transaction.TransactionManager manager) {
        return new ManagerAdapter(manager);
    }

    private static SystemException systemException(Exception exc) {
        SystemException toThrow = new SystemException(exc.getMessage());
        toThrow.initCause(exc);
        return toThrow;
    }

    private static RollbackException rollbackException(Exception exc) {
        RollbackException toThrow = new RollbackException(exc.getMessage());
        toThrow.initCause(exc);
        return toThrow;
    }

    private static HeuristicMixedException heuristicMixedException(Exception exc) {
        HeuristicMixedException toThrow = new HeuristicMixedException(exc.getMessage());
        toThrow.initCause(exc);
        return toThrow;
    }

    private static HeuristicRollbackException heuristicRollbackException(Exception exc) {
        HeuristicRollbackException toThrow = new HeuristicRollbackException(exc.getMessage());
        toThrow.initCause(exc);
        return toThrow;
    }

    /**
     * Transaction manager adapter.
     */
    private static class ManagerAdapter implements TransactionManager {
        private final jakarta.transaction.TransactionManager delegate;
        // DBCP keys its connection administration on the transaction object, so the same adapter
        // must be returned for the same transaction. Entries are removed at the end of the transaction.
        private final Map<jakarta.transaction.Transaction, TransactionAdapter> adapters = new HashMap<>();

        ManagerAdapter(jakarta.transaction.TransactionManager delegate) {
            this.delegate = delegate;
        }

        @Override
        public void begin() throws NotSupportedException, SystemException {
            try {
                delegate.begin();
            } catch (jakarta.transaction.NotSupportedException exc) {
                NotSupportedException toThrow = new NotSupportedException(exc.getMessage());
                toThrow.initCause(exc);
                throw toThrow;
            } catch (jakarta.transaction.SystemException exc) {
                throw systemException(exc);
            }
        }

        @Override
        public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException,
                SecurityException, IllegalStateException, SystemException {
            try {
                delegate.commit();
            } catch (jakarta.transaction.RollbackException exc) {
                throw rollbackException(exc);
            } catch (jakarta.transaction.HeuristicMixedException exc) {
                throw heuristicMixedException(exc);
            } catch (jakarta.transaction.HeuristicRollbackException exc) {
                throw heuristicRollbackException(exc);
            } catch (jakarta.transaction.SystemException exc) {
                throw systemException(exc);
            }
        }

        @Override
        public int getStatus() throws SystemException {
            try {
                return delegate.getStatus();
            } catch (jakarta.transaction.SystemException exc) {
                throw systemException(exc);
            }
        }

        @Override
        public Transaction getTransaction() throws SystemException {
            jakarta.transaction.Transaction transaction;
            try {
                transaction = delegate.getTransaction();
                // Some transaction managers return a transaction object even if no transaction is active.
                if (transaction == null || transaction.getStatus() == jakarta.transaction.Status.STATUS_NO_TRANSACTION) {
                    return null;
                }
            } catch (jakarta.transaction.SystemException exc) {
                throw systemException(exc);
            }
            synchronized (adapters) {
                TransactionAdapter adapter = adapters.get(transaction);
                if (adapter != null) {
                    return adapter;
                }
                adapter = new TransactionAdapter(transaction);
                try {
                    final jakarta.transaction.Transaction key = transaction;
                    transaction.registerSynchronization(new jakarta.transaction.Synchronization() {
                        @Override
                        public void beforeCompletion() {
                            // Nothing to do.
                        }

                        @Override
                        public void afterCompletion(int status) {
                            synchronized (adapters) {
                                adapters.remove(key);
                            }
                        }
                    });
                    adapters.put(transaction, adapter);
                } catch (Exception exc) {
                    // Transaction not active anymore: the adapter is not cached.
                }
                return adapter;
            }
        }

        @Override
        public void resume(Transaction transaction) throws InvalidTransactionException, IllegalStateException,
                SystemException {
            if (!(transaction instanceof TransactionAdapter)) {
                throw new InvalidTransactionException("unknown transaction: " + transaction);
            }
            try {
                delegate.resume(((TransactionAdapter) transaction).delegate);
            } catch (jakarta.transaction.InvalidTransactionException exc) {
                InvalidTransactionException toThrow = new InvalidTransactionException(exc.getMessage());
                toThrow.initCause(exc);
                throw toThrow;
            } catch (jakarta.transaction.SystemException exc) {
                throw systemException(exc);
            }
        }

        @Override
        public void rollback() throws IllegalStateException, SecurityException, SystemException {
            try {
                delegate.rollback();
            } catch (jakarta.transaction.SystemException exc) {
                throw systemException(exc);
            }
        }

        @Override
        public void setRollbackOnly() throws IllegalStateException, SystemException {
            try {
                delegate.setRollbackOnly();
            } catch (jakarta.transaction.SystemException exc) {
                throw systemException(exc);
            }
        }

        @Override
        public void setTransactionTimeout(int seconds) throws SystemException {
            try {
                delegate.setTransactionTimeout(seconds);
            } catch (jakarta.transaction.SystemException exc) {
                throw systemException(exc);
            }
        }

        @Override
        public Transaction suspend() throws SystemException {
            try {
                jakarta.transaction.Transaction transaction = delegate.suspend();
                return (transaction == null) ? null : new TransactionAdapter(transaction);
            } catch (jakarta.transaction.SystemException exc) {
                throw systemException(exc);
            }
        }
    }

    /**
     * Transaction adapter. Equality is that of the adapted transaction.
     */
    private static class TransactionAdapter implements Transaction {
        private final jakarta.transaction.Transaction delegate;

        TransactionAdapter(jakarta.transaction.Transaction delegate) {
            this.delegate = delegate;
        }

        @Override
        public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException,
                SecurityException, IllegalStateException, SystemException {
            try {
                delegate.commit();
            } catch (jakarta.transaction.RollbackException exc) {
                throw rollbackException(exc);
            } catch (jakarta.transaction.HeuristicMixedException exc) {
                throw heuristicMixedException(exc);
            } catch (jakarta.transaction.HeuristicRollbackException exc) {
                throw heuristicRollbackException(exc);
            } catch (jakarta.transaction.SystemException exc) {
                throw systemException(exc);
            }
        }

        @Override
        public boolean delistResource(XAResource resource, int flag) throws IllegalStateException, SystemException {
            try {
                return delegate.delistResource(resource, flag);
            } catch (jakarta.transaction.SystemException exc) {
                throw systemException(exc);
            }
        }

        @Override
        public boolean enlistResource(XAResource resource) throws RollbackException, IllegalStateException,
                SystemException {
            try {
                return delegate.enlistResource(resource);
            } catch (jakarta.transaction.RollbackException exc) {
                throw rollbackException(exc);
            } catch (jakarta.transaction.SystemException exc) {
                throw systemException(exc);
            }
        }

        @Override
        public int getStatus() throws SystemException {
            try {
                return delegate.getStatus();
            } catch (jakarta.transaction.SystemException exc) {
                throw systemException(exc);
            }
        }

        @Override
        public void registerSynchronization(Synchronization sync) throws RollbackException, IllegalStateException,
                SystemException {
            try {
                delegate.registerSynchronization(new jakarta.transaction.Synchronization() {
                    @Override
                    public void beforeCompletion() {
                        sync.beforeCompletion();
                    }

                    @Override
                    public void afterCompletion(int status) {
                        sync.afterCompletion(status);
                    }
                });
            } catch (jakarta.transaction.RollbackException exc) {
                throw rollbackException(exc);
            } catch (jakarta.transaction.SystemException exc) {
                throw systemException(exc);
            }
        }

        @Override
        public void rollback() throws IllegalStateException, SystemException {
            try {
                delegate.rollback();
            } catch (jakarta.transaction.SystemException exc) {
                throw systemException(exc);
            }
        }

        @Override
        public void setRollbackOnly() throws IllegalStateException, SystemException {
            try {
                delegate.setRollbackOnly();
            } catch (jakarta.transaction.SystemException exc) {
                throw systemException(exc);
            }
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof TransactionAdapter && ((TransactionAdapter) obj).delegate.equals(delegate);
        }

        @Override
        public int hashCode() {
            return delegate.hashCode();
        }

        @Override
        public String toString() {
            return "javax.transaction adapter for " + delegate;
        }
    }
}
