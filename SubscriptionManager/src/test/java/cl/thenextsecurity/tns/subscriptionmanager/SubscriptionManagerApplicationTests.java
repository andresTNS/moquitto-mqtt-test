package cl.thenextsecurity.tns.subscriptionmanager;

import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

/**
 * Suite completa de tests para SubscriptionManager.
 *
 * Esta clase ejecuta automáticamente todos los tests del proyecto presentes
 * y futuros bajo el paquete cl.thenextsecurity.tns.subscriptionmanager.
 *
 * Uso en IDE:
 *   - IntelliJ/Eclipse: click derecho en esta clase → "Run" ejecuta todos los tests.
 *
 * Uso en Maven:
 *   - mvn test: ejecuta cada test individualmente + la suite (doble ejecución).
 *   - Para ejecutar solo la suite y evitar duplicados, configurar maven-surefire-plugin
 *     con <includes><include>** /SubscriptionManagerApplicationTests.java</include></includes>
 *
 * Nota: Cualquier nueva clase de test agregada bajo este paquete se incluye
 * automáticamente sin modificar esta suite.
 */
@Suite
@SuiteDisplayName("SubscriptionManager — Suite completa de tests")
@SelectPackages("cl.thenextsecurity.tns.subscriptionmanager")
class SubscriptionManagerApplicationTests {
    // Esta clase no necesita métodos @Test.
    // La anotación @Suite + @SelectPackages descubre y ejecuta todos los tests
    // bajo el paquete especificado automáticamente.
}
