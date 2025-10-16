import org.gic.readFile
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class MainKtTest {

    @Test
    fun `readFile should return correct number of test data items`() {
        val result = readFile().size
        
        assertEquals(2, result)
    }
}