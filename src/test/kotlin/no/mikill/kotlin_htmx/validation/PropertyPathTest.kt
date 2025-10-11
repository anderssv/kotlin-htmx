package no.mikill.kotlin_htmx.validation

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PropertyPathTest {
    data class TestPerson(
        val name: String,
        val address: TestAddress,
        val addresses: List<TestAddress>,
    )

    data class TestAddress(
        val city: String,
    )

    @Test
    fun `PropertyPath Direct should hold property and generate correct path string`() {
        // Arrange
        val nameProperty = TestPerson::name

        // Act
        val propertyPath = PropertyPath.Direct(nameProperty)

        // Assert
        assertThat(propertyPath.property).isEqualTo(nameProperty)
        assertThat(propertyPath.path).isEqualTo("name")
    }

    @Test
    fun `PropertyPath Nested should combine parent and child paths with a dot`() {
        // Arrange
        val addressProperty = TestPerson::address
        val cityProperty = TestAddress::city
        val parentPath = PropertyPath.Direct(addressProperty)

        // Act
        val nestedPath = PropertyPath.Nested(parentPath, cityProperty)

        // Assert
        assertThat(nestedPath.path).isEqualTo("address.city")
        assertThat(nestedPath.property).isEqualTo(cityProperty)
    }

    @Test
    fun `PropertyPath Indexed should generate path like addresses bracket 1 bracket dot city`() {
        // Arrange
        val addressesProperty = TestPerson::addresses
        val cityProperty = TestAddress::city
        val index = 1

        // Act
        val indexedPath = PropertyPath.Indexed(addressesProperty, index, cityProperty)

        // Assert
        assertThat(indexedPath.path).isEqualTo("addresses[1].city")
        assertThat(indexedPath.elementProperty).isEqualTo(cityProperty)
    }

    @Test
    fun `toPath extension should create PropertyPath Direct`() {
        // Arrange & Act
        val propertyPath = TestPerson::name.toPath()

        // Assert
        assertThat(propertyPath).isInstanceOf(PropertyPath.Direct::class.java)
        assertThat(propertyPath.path).isEqualTo("name")
    }

    @Test
    fun `then extension should create PropertyPath Nested`() {
        // Arrange
        val parentPath = TestPerson::address.toPath()

        // Act
        val nestedPath = parentPath.then(TestAddress::city)

        // Assert
        assertThat(nestedPath).isInstanceOf(PropertyPath.Nested::class.java)
        assertThat(nestedPath.path).isEqualTo("address.city")
    }

    @Test
    fun `at extension should create PropertyPath Indexed with correct path`() {
        // Arrange & Act
        val indexedPath = TestPerson::addresses.at(0, TestAddress::city)

        // Assert
        assertThat(indexedPath).isInstanceOf(PropertyPath.Indexed::class.java)
        assertThat(indexedPath.path).isEqualTo("addresses[0].city")
    }

    @Test
    fun `PropertyPath works with real domain classes combining all path types`() {
        // Test Direct path
        val namePath = TestPerson::name.toPath()
        assertThat(namePath.path).isEqualTo("name")

        // Test Nested path using then()
        val cityPath = TestPerson::address.toPath().then(TestAddress::city)
        assertThat(cityPath.path).isEqualTo("address.city")

        // Test Indexed path using at()
        val indexedCityPath = TestPerson::addresses.at(1, TestAddress::city)
        assertThat(indexedCityPath.path).isEqualTo("addresses[1].city")

        // Test multiple indexes
        val firstAddressCity = TestPerson::addresses.at(0, TestAddress::city)
        val secondAddressCity = TestPerson::addresses.at(1, TestAddress::city)
        assertThat(firstAddressCity.path).isEqualTo("addresses[0].city")
        assertThat(secondAddressCity.path).isEqualTo("addresses[1].city")
    }

    @Test
    fun `PropertyPath Direct getValue should extract value from object`() {
        // Arrange
        val person = TestPerson(name = "John", address = TestAddress("Boston"), addresses = emptyList())
        val namePath = TestPerson::name.toPath()

        // Act
        val value = namePath.getValue(person)

        // Assert
        assertThat(value).isEqualTo("John")
    }

    @Test
    fun `PropertyPath Nested getValue should navigate through nested properties`() {
        // Arrange
        val person = TestPerson(name = "John", address = TestAddress("Boston"), addresses = emptyList())
        val cityPath = TestPerson::address.toPath().then(TestAddress::city)

        // Act
        val value = cityPath.getValue(person)

        // Assert
        assertThat(value).isEqualTo("Boston")
    }

    @Test
    fun `PropertyPath Indexed getValue should navigate to list element property`() {
        // Arrange
        val addresses = listOf(TestAddress("Boston"), TestAddress("Portland"), TestAddress("Seattle"))
        val person = TestPerson(name = "John", address = TestAddress("Boston"), addresses = addresses)
        val indexedPath = TestPerson::addresses.at(1, TestAddress::city)

        // Act
        val value = indexedPath.getValue(person)

        // Assert
        assertThat(value).isEqualTo("Portland")
    }
}
