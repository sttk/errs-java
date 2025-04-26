import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

public class SampleTest {

    @Test
    void hello() {
        var sample = new Sample();
        assertThat(sample.hello()).isEqualTo("Hello");
    }
}
