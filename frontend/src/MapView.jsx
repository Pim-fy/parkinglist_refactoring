import { useEffect, useRef } from "react";    // useEffect : 렌더링 후 실행할 작업. useRef : 특정 데이터를 렌더링과 독립적으로 저장, 참조할 수 있게 해주는 훅.

// 기본 중심좌표 설정. 현재 전주시청.
const DEFAULT_LAT = 35.8242238;     // 위도.
const DEFAULT_LON = 127.1479532;    // 경도.
const DEFAULT_ZOOM = 15;            // 지도 확대 수준.

function MapView({ onMapReady }) {
  // --- useRef 모음.
  // useEffect가 다시 실행돼도 값을 유지할 수 있음.

  const mapRef = useRef(null);              // 지도가 그려질 영역.
  const mapInstanceRef = useRef(null);      // 지도 객체.
  const userMarkerRef = useRef(null);       // 유저 위치 표시 마커.
  const watchIdRef = useRef(null);          // GPS 추적 ID 저장.

  // --- useRef 모음 종료.


  // --- useEffect 시작.

  useEffect(() => {   // onMapReady 전달, 상태 변경 시 작동.
    // Tmap 객체 추출. window에서 Tmapv2라는 도구를 꺼내오는 것.
    const { Tmapv2 } = window;

    // --- 실행 전 안전장치. ---

    // Tmap객체 접근 불가 or 리액트가 DOM에 렌더링 전이라면 실행 중단.
    if (!Tmapv2 || !mapRef.current) return;

    // 지도 중복 생성 방지.
    // firstChild로 map_div안에 자식 노드(내용물)이 있는지 확인.
    if (mapRef.current.firstChild) return;

    // --- 실행 전 안전장치 종료. ---


    // --- 함수 모음. ---

    // 지도 객체 생성, 배포, 객체 전달 함수.
    const initMap = () => {
      // 지도 객체 생성.
      const map = new Tmapv2.Map("map_div", {
        center: new Tmapv2.LatLng(DEFAULT_LAT, DEFAULT_LON),  // 기본 좌표로 먼저 로드.
        width: "100vw",                                       // 뷰표인트 사용. 전체 너비.
        height: "100vh",                                      // 뷰포인트 사용. 전체 높이.
        zoom: DEFAULT_ZOOM,
        zoomControl: false,                                   // 줌 막대 비활성화.
      });

      // 맵 인스턴스를 ref에 저장.
      mapInstanceRef.current = map; 

      // onMapReady 함수 실행. App.jsx에서의 handleMapReady.
      // map 객체와 Tmapv2 객체 전달.
      // if문 사용으로 안전장치 역할.
      if (onMapReady) {
        onMapReady(map, Tmapv2);
      }

      // 사용자의 위치 추적을 시작.
      startLocationTracking(map, Tmapv2);
    };

    // 위치 업데이트.
    const handlePositionUpdate = (position, map, Tmapv2) => {
      
      const userLat = position.coords.latitude;     // 위치 정보(위도) 추출.
      const userLon = position.coords.longitude;    // 위치 정보(경도) 추출.
      
      // 새로운 좌표 객체 만들고 저장.
      const newPosition = new Tmapv2.LatLng(userLat, userLon);    // LatLng : tmap sdk의 생성자함수.

      console.log("유저 위치 업데이트:", userLat, userLon);

      // 사용자의 현 위치 지도에 표시. 마커 생성 or 이동.
      // 처음 위치를 받았을 때, 이후 위치가 변경될 때를 if문으로 구분하여 마커 생성or이동 결정
      if (!userMarkerRef.current) {                       // 첫 위치 수신, 마커 생성.
        userMarkerRef.current = new Tmapv2.Marker({       // Marker : tmap sdk의 생성자함수.
          position: newPosition,                          // 마커 위치.
          map: map                                        // 지도 객체.
          // icon: "/marker-user.png"                     // 아이콘.
          // iconSize: new Tmapv2.Size(24, 38)            // 아이콘 사이즈.
        });
        map.setCenter(newPosition);                       // 마커 위치를 화면 중심으로 설정.
      } else {                                            // 위치 변경 수신, 마커 이동.
        userMarkerRef.current.setPosition(newPosition);   // 마커의 위치(position) 변경.
      }
    };

    // 위치 추적 실패.
    const handleError = (error) => {
      console.error("위치 추적 오류 :", error.message);     // 콘솔에 에러 로그 출력.
    };

    // 위치 추적 시작
    const startLocationTracking = (map, Tmapv2) => {
      if (navigator.geolocation) {                                      // 브라우저 내장기능(Web API).
        watchIdRef.current = navigator.geolocation.watchPosition(       // watchPosition : 위치가 바뀔 때마다 알려줌.
          (position) =>                                                 // 매개변수로 position을 받아 위치 업데이트 or 위치 추적 실패 함수 실행.
            handlePositionUpdate(position, map, Tmapv2),                // 성공 콜백.
            handleError,                                                // 실패 콜백.
          {                                                             // 옵션 객체.
            enableHighAccuracy: true,                                   // 정확도.
            timeout: 10000,                                             // 응답 대기 시간. 10초. 단위 ms.
            maximumAge: 0                                               // 위치 정보 유효 시간.
          }
        );
      } else {                                                          // geolocation 지원하지 않는 경우.
        console.warn("이 브라우저는 Geolocation을 지원하지 않습니다."); // 콘솔에 경고 출력.
      }
    };

    // --- 함수 모음 종료. ---

    
    // 지도 객체 생성.
    initMap();
    
    // Cleanup 함수. 컴포넌트가 사라질 때 실행.
    // useEffect 훅이 함수 반환 시, 리액트는 컴포넌트가 제거되거나 다음 효과 실행 직전에 이 반환 함수 실행.
    return () => {
      // 추적 ID 존재 확인
      if (watchIdRef.current) {
        navigator.geolocation.clearWatch(watchIdRef.current); // watchPosition 종료.
      }
      // 지도 객체 존재 확인
      if (mapInstanceRef.current) {
        mapInstanceRef.current.destroy();   // 지도 객체 파괴.
        mapInstanceRef.current = null;      // 참조 제거.
      }
    };

    
  }, [onMapReady]);

  // --- useEffect 종료.

  // 최종 반환 JSX.
  // 화면에 지도를 표시하기 위해 반환하는 최종 JSX 구조
  return (
    <div
      id="map_div"
      ref={mapRef}
      style={{ position: "fixed", inset: 0, width: "100vw", height: "100vh", zIndex: 0 }}
    />
  );
}

export default MapView;   // 이 파일을 App.jsx에서 불러와 쓸 수 있게 공개.
