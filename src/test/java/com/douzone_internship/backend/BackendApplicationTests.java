package com.douzone_internship.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// 기동 배치는 src/test/resources/application.properties에서 전역으로 꺼둔다.
// (켜져 있으면 이 테스트만으로도 실제 data.go.kr을 11회 호출한다)
@SpringBootTest
class BackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
