package osgi.extender.jpa.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

class PersistenceUnitDefinitionTest {
    private static final String JAKARTA = "https://jakarta.ee/xml/ns/persistence";

    private static List<PersistenceUnitDefinition> parse(String namespace, String version, String units) throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<persistence xmlns=\"" + namespace + "\" version=\"" + version + "\">\n"
                + units
                + "</persistence>\n";
        return PersistenceUnitDefinition.fromFile(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    @ParameterizedTest
    @CsvSource({
        "http://java.sun.com/xml/ns/persistence, 2.0",
        "http://xmlns.jcp.org/xml/ns/persistence, 2.1",
        "https://jakarta.ee/xml/ns/persistence, 3.2"
    })
    void parsesAllSchemaNamespaces(String namespace, String version) throws Exception {
        List<PersistenceUnitDefinition> units = parse(namespace, version,
                "  <persistence-unit name=\"a\" transaction-type=\"RESOURCE_LOCAL\">\n"
                + "    <provider>\n      org.eclipse.persistence.jpa.PersistenceProvider\n    </provider>\n"
                + "    <non-jta-data-source>osgi:service/javax.sql.DataSource/(name=test)</non-jta-data-source>\n"
                + "    <class>x.A</class>\n"
                + "    <class>x.B</class>\n"
                + "    <properties>\n"
                + "      <property name=\"k1\" value=\"v1\"/>\n"
                + "      <property name=\"k2\" value=\"v2\"/>\n"
                + "    </properties>\n"
                + "  </persistence-unit>\n"
                + "  <!-- comment -->\n"
                + "  <persistence-unit name=\"b\"/>\n");
        assertEquals(2, units.size());
        PersistenceUnitDefinition a = units.get(0);
        assertEquals("a", a.name);
        assertEquals(version, a.version);
        assertEquals("RESOURCE_LOCAL", a.transactionType);
        assertEquals("org.eclipse.persistence.jpa.PersistenceProvider", a.provider);
        assertEquals("osgi:service/javax.sql.DataSource/(name=test)", a.nonJtaDs);
        assertEquals(List.of("x.A", "x.B"), a.classes);
        assertEquals(2, a.properties.size());
        assertEquals("k2", a.properties.get(1).key);
        assertEquals("v2", a.properties.get(1).value);
        PersistenceUnitDefinition b = units.get(1);
        assertEquals("b", b.name);
        assertEquals("", b.transactionType);
        assertTrue(b.classes.isEmpty());
        assertTrue(b.properties.isEmpty());
    }

    /**
     * The example of the user manual (section 3.3.2): the data source reference is surrounded by
     * white space, which must not end up in the reference.
     */
    @Test
    void parsesManualExample() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<persistence version=\"2.1\" xmlns=\"http://xmlns.jcp.org/xml/ns/persistence\"\n"
                + "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                + "  xsi:schemaLocation=\"http://xmlns.jcp.org/xml/ns/persistence\n"
                + "    http://xmlns.jcp.org/xml/ns/persistence/persistence_2_1.xsd\">\n"
                + "  <persistence-unit name=\"Orders\" transaction-type=\"JTA\">\n"
                + "    <jta-data-source>\n"
                + "      osgi:service/javax.sql.DataSource/(name=OrdersDS)\n"
                + "    </jta-data-source>\n"
                + "    <class>orders.objects.Order</class>\n"
                + "    <exclude-unlisted-classes>true</exclude-unlisted-classes>\n"
                + "  </persistence-unit>\n"
                + "</persistence>\n";
        List<PersistenceUnitDefinition> units = PersistenceUnitDefinition.fromFile(
                new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        assertEquals(1, units.size());
        PersistenceUnitDefinition unit = units.get(0);
        assertEquals("Orders", unit.name);
        assertEquals("JTA", unit.transactionType);
        assertEquals("osgi:service/javax.sql.DataSource/(name=OrdersDS)", unit.jtaDs);
        assertEquals(List.of("orders.objects.Order"), unit.classes);
        assertTrue(unit.excludeUnlisted);
    }

    @Test
    void ignoresUnknownNamespace() throws Exception {
        assertTrue(parse("urn:unknown", "1.0", "<persistence-unit name=\"a\"/>").isEmpty());
    }

    @ParameterizedTest
    @CsvSource({
        "'', false",
        "<exclude-unlisted-classes/>, true",
        "<exclude-unlisted-classes>true</exclude-unlisted-classes>, true",
        "<exclude-unlisted-classes> false </exclude-unlisted-classes>, false"
    })
    void excludeUnlistedClasses(String element, boolean expected) throws Exception {
        List<PersistenceUnitDefinition> units = parse(JAKARTA, "3.2",
                "<persistence-unit name=\"a\">" + element + "</persistence-unit>");
        assertEquals(expected, units.get(0).excludeUnlisted);
    }

    @Test
    void scopeAndQualifiers() throws Exception {
        PersistenceUnitDefinition unit = parse(JAKARTA, "3.2",
                "<persistence-unit name=\"a\">"
                + "<qualifier>x.Q1</qualifier><qualifier>x.Q2</qualifier>"
                + "<scope>jakarta.enterprise.context.ApplicationScoped</scope>"
                + "</persistence-unit>").get(0);
        assertEquals("jakarta.enterprise.context.ApplicationScoped", unit.scope);
        assertEquals(List.of("x.Q1", "x.Q2"), unit.qualifiers);
    }

    @Test
    void unitsAreEqualByName() throws Exception {
        List<PersistenceUnitDefinition> units = parse(JAKARTA, "3.2",
                "<persistence-unit name=\"a\"/><persistence-unit name=\"a\"/>");
        assertEquals(units.get(0), units.get(1));
        assertFalse(units.get(0).equals(null));
    }
}
