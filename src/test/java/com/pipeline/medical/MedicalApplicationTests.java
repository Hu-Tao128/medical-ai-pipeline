package com.pipeline.medical;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MedicalApplicationTests {

	@Test
	void applicationClassExists() {
		assertDoesNotThrow(() -> Class.forName("com.pipeline.medical.MedicalApplication"));
	}

}
