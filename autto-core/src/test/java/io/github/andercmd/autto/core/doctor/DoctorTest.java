package io.github.andercmd.autto.core.doctor;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class DoctorTest {

    @Test
    void javaCheckPassesOnASupportedJdk() {
        assertThat(Doctor.java().status()).isEqualTo(Doctor.Status.OK);
    }

    @Test
    void everyCheckHasANameAndRenders() {
        List<Doctor.Check> checks = Doctor.run();

        assertThat(checks).isNotEmpty().allSatisfy(check -> {
            assertThat(check.name()).isNotBlank();
            assertThat(check.render()).contains(check.name());
        });
        assertThat(checks).extracting(Doctor.Check::name).contains("Java", "Configuration", "Docker");
    }
}
