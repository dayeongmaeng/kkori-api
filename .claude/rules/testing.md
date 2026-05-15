# 테스트 규칙

## 단위 테스트
- **프레임워크**: JUnit 5 + Mockito + AssertJ
- **위치**: `src/test/java/com/kkori/api/{domain}/`
- **명명**: `{ClassName}Test.java`
- **패턴**: given / when / then 명확히 구분

```java
@Test
void shouldReturnPetWhenValidIdGiven() {
    // given
    Long petId = 1L;
    Pet pet = Pet.builder().name("탱이").build();
    given(petRepository.findById(petId)).willReturn(Optional.of(pet));

    // when
    PetResponse result = petService.findById(petId);

    // then
    assertThat(result.getName()).isEqualTo("탱이");
}
```

## 통합 테스트
- **어노테이션**: `@SpringBootTest`
- **DB**: Testcontainers (PostgreSQL)
- **위치**: `src/test/java/com/kkori/api/{domain}/integration/`
- **명명**: `{Feature}IntegrationTest.java`

## 컨트롤러 테스트
- **MockMvc**: `@WebMvcTest` + `@MockBean`
- HTTP 요청/응답 검증
- 응답 포맷 검증 (ApiResponse 구조)

## 커버리지 목표
- 서비스 레이어: 80% 이상
- 컨트롤러: 주요 시나리오 (성공/실패)
- 리포지토리: 커스텀 쿼리만

## 테스트 데이터
- Builder 패턴으로 생성
- 공통 fixture는 `test/java/.../fixture/` 폴더
- 한국어 데이터 사용 (실제 사용자 톤)