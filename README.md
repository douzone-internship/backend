# 얼마닥 (EolmaDoc) - Frontend

## 프로젝트 개요
건강보험심사평가원의 비급여 진료비 공공데이터를 활용하여 사용자에게 투명한 의료 가격 정보와 AI 분석 리포트를 제공하는 서비스의 프론트엔드 저장소입니다.

## 기술 스택
- Framework: React 19.2.0
- Styling: TailwindCSS 3.4.1
- HTTP Client: Axios 1.13.2
- Routing: React Router Dom 7.9.5
- Icons: React Icons 5.5.0

## 주요 기능
- 메인 검색: 진료명(필수), 병원명, 지역을 선택하여 가격 비교를 요청합니다.
- 결과 시각화: 검색된 병원 목록을 가격순으로 정렬하여 확인하고, Gemini AI가 생성한 가격 분석 리포트를 열람합니다.
- 사용자 인터페이스: 재사용 가능한 UI 컴포넌트(AutocompleteInput, Badge, Skeleton 등)를 활용하여 일관된 UX를 제공합니다.

## 주요 화면 구성
- 홈 화면: 서비스 소개 및 통합 검색 바
- 결과 화면: 병원 리스트 및 AI 추천 섹션
- 회원가입/로그인: 이메일 기반 계정 관리
- 내 정보 화면: 과거 검색 기록 및 AI 분석 결과 관리

## 시작하기
1. 의존성 설치: npm install
2. 로컬 실행: npm start
3. 빌드: npm run build
