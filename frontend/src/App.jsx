import React, { useState, useEffect, useCallback } from "react"; 
import MapView from "./MapView";
import "./App.css"; 
import axios from "axios";

const API_BASE_URL = "http://localhost:8080";

function App() {
  // --- useState 정의(상태 정의). ---

  // 회원 관련 상태.
  const [isModalOpen, setIsModalOpen] = useState(false);    // 로그인, 회원가입 모달창 열림/닫힘 상태.
  const [authMode, setAuthMode] = useState('login');        // 로그인 상태. login/signup.
  const [username, setUsername] = useState('');             // 로그인, 회원가입 시 입력되는 username 저장.
  const [password, setPassword] = useState('');             // 로그인, 회원가입 시 입력되는 password 저장.
  const [email, setEmail] = useState('');                   // 로그인, 회원가입 시 입력되는 email 저장.
  const [userType, setUserType] = useState('일반회원');     // 회원가입 시 회원 구분 상태. 일반회원/사업자회원.
  const [isLoggedIn, setIsLoggedIn] = useState(false);      // 현재 사용자의 로그인 성공 여부.
  const [userRole, setUserRole] = useState(null);           // 로그인한 사용자의 권한을 저장. ROLE_USER / ROLE_BUSINESS

  // 지도 및 장소 검색.
  const [map, setMap] = useState(null);                                     // T map 객체를 저장할 상태.
  const [isTmapReady, setIsTmapReady] = useState(false);                    // T map 라이브러리와 Geocoder API 완전히 로드되었는지 확인.
  const [keyword, setKeyword] = useState("");                               // 검색창에 입력한 목적지 검색어 저장.
  const [places, setPlaces] = useState([]);                                 // T map API로 받아온 목적지 검색 결과 목록(배열)을 저장.
  const [selectedDestination, setSelectedDestination] = useState(null);     // 검색 결과 중 사용자가 최종 선택한 목적지의 이름, 주소, 좌표 정보 저장.
  const [searchMarkers, setSearchMarkers] = useState([]);                   // 현재 지도 위에 표시되고 있는 모든 마커 객체들을 배열로 저장, 관리.
  const [infoWindow, setInfoWindow] = useState(null);                       // 지도 위에 떠 있는 정보창 객체를 저장하여 하나만 열리도록 제어.
  
  // 주차장 검색 및 결과.
  const [parkingLots, setParkingLots] = useState([]);                           // 주차장 검색 결과 목록.
  const [filterOptions, setFilterOptions] = useState({                          // 주차장 검색 필터의 체크 상태를 객체로 관리.
    isFree: false,
    isPublic: false,
  });
  const [selectedParkingDetail, setSelectedParkingDetail] = useState(null);     // 상세 모달에 표시할 특정 주차장의 상세 정보 객체 저장.
  const [isParkingLoading, setIsParkingLoading] = useState(false);              // 주차장 목록을 불러오는 중인지 나타내는 로딩 상태.
  const [isDetailLoading, setIsDetailLoading] = useState(false);                // 특정 주차장의 추가 상세 정보를 백엔드에서 가져오는 중인지 나타내는 로딩 상태.
  
  // 사업자 전용 관리.
  const [isBusinessModalOpen, setIsBusinessModalOpen] = useState(false);      // 사업자 마이페이지 모달의 열림 상태.
  const [myLocations, setMyLocations] = useState([]);                         // 해당 사업자가 등록한 모든 사업장 목록 저장.
  const [currentLocationId, setCurrentLocationId] = useState(null);           // 현재 수정중이거나, 주차장을 등록하려는 사업장의 ID를 저장.
  const [myLocationParkings, setMyLocationParkings] = useState([]);           // 특정 사업장에 이미 등록되어 있는 추천 주차장 목록.
  const [myPageModalDetail, setMyPageModalDetail] = useState(null);           // 모달에 띄울 parking 객체.
  const [myPageModalText, setMyPageModalText] = useState("");                 // 모달 안의 '추가 정보' 입력값.
  
  // 수동 주차장 등록.
  const [manualSearchKeyword, setManualSearchKeyword] = useState("");         // 마이페이지 내 주차장 검색창에 입력한 값.
  const [manualSearchResults, setManualSearchResults] = useState([]);         // 수동 검색으로 찾은 주차장 결과 목록.
  const [manualSelectedPlace, setManualSelectedPlace] = useState(null);       // 검색 결과 중 수동으로 등록하기 위해 선택한 장소 정보.
  const [manualParkingName, setManualParkingName] = useState("");             // 사용자가 직접 입력한 등록용 주차장 이름.
  const [newParkingInfo, setNewParkingInfo] = useState("");                   // 주차장 등록 시 입력하는 추가 정보 텍스트.
  const [newParkingAddress, setNewParkingAddress] = useState("");             // 수동 주소 등록용 입력값.
  // const [searchParkingList, setSearchParkingList] = useState([]);

  // --- useState 정의(상태 정의) 종료. ---



  // --- useEffect 모음. ---

  // 로그인 상태 확인. 앱 구동 시 로컬 스토리지의 정보 확인 후 로그인 상태 복구.
  useEffect(() => {                                   // 빈 배열이므로 켜질 때 한번만 실행.
    const token = localStorage.getItem("token");      // 사용자가 이전에 로그인했을 때 저장해둔 인증 토큰을 변수에 저장. 저장된 값이 없다면 null.
    const storedRole = localStorage.getItem("role");  // 현재 로그인한 사용자의 권한을 확인하여 변수에 저장.
    if (token) { 
      setIsLoggedIn(true);                            // 토큰이 존재하는 경우 로그인 상태 변경.
      if(storedRole) {
        setUserRole(storedRole);                      // 로컬에 저장된 권한이 존재하는 경우 권한 변경.
        if (storedRole === 'ROLE_BUSINESS') {
              fetchMyLocations(token);                // 권한이 사업자인 경우, 토큰을 사용해 fetchMyLocations 함수를 실행시켜 사업장 목록을 불러옵니다.
        }
      }
    }
  },[]);
  
  // 사업자 마이페이지 모달이 열릴 경우, 모달 내부 데이터를 초기화.
  useEffect(() => {                   // 사업자 마이페이지 모달이 열림 상태가 바뀔 때마다 실행.
    if (isBusinessModalOpen) {
      setCurrentLocationId(null);     // 이전에 선택했던 사업장 ID.
      setMyLocationParkings([]);      // 사업장에 등록되어 있는 추천 주차장 목록.
      setManualSearchKeyword("");     // 수동 등록 검색창에 입력한 값.
      setManualSearchResults([]);     // 수동 등록 검색 결과 목록.
      setManualSelectedPlace(null);   // 수동 등록 검색 결과 목록 중 선택했던 장소 정보.
      setManualParkingName("");       // 직접 입력한 등록용 주차장 이름.
      setNewParkingInfo("");          // 주차장 등록 시 입력하는 추가 정보 텍스트.
    }
  }, [isBusinessModalOpen]);

  // 사업자 유저의 로그인 시 지도 이동.
  useEffect(() => {                   // myLocations or map의 상태가 변경될 때마다 실행.
    // 사업장 목록이 비어있거나, 지도가 준비되지 않은 경우 실행 중단.
    if (!myLocations || myLocations.length === 0 || !map) {
      return; 
    }

    // 로그인 시 지도 이동.
    // 목적지가 아직 선택되지 않았고, 사용자가 사업자일 때만 실행.
    if (!selectedDestination && userRole === 'ROLE_BUSINESS') {
      const firstLocation = myLocations[0];   // 첫 번째 사업장의 좌표를 가져옴.
      const point = new window.Tmapv2.LatLng(firstLocation.latitude, firstLocation.longitude);    // 지도의 특정 지점을 가리키는 좌표 객체를 생성.
      
      // 지도를 해당 좌표로 이동, 확대. Tmap API의 기본 내장 함수(메서드) 사용.
      map.setCenter(point);
      map.setZoom(16);
    }
  }, [myLocations, map]);

  // Tmapv2 기본 객체가 로드된 후, Tmapv2, Geocoder 두 객체가 모두 존재하는지 확인.
  useEffect(() => {                                                   // 빈 배열이므로 켜질 때 한번만 실행.
    // Tmapv2 객체, 그 하위 Geocoder 객체가 모두 존재하는지 확인.
    const checkGeocoderReady = () => {
        if (window.Tmapv2 && window.Tmapv2.Geocoder) {
            setIsTmapReady(true);                                     // 두 객체가 모두 존재하는 경우 state 변경.
            console.log("Tmap API 로드 성공. Geocoder 준비 완료.");
        } else {
            console.log("Tmap API 로딩중. 0.5초 간격 확인.")
            setTimeout(checkGeocoderReady, 500);                      // Geocoder가 로드되지 않았다면 500ms(0.5초) 후에 다시 체크.
        }
    };
    
    // Tmapv2 기본 객체가 로드된 후에 체크 시작.
    if (window.Tmapv2) {
        checkGeocoderReady();                                         // checkGeocoderReady 함수 실행.
    }
  }, []);

  // 사업자가 자신의 사업장 주변 주차장 관리 시 작동하는 로직. 
  // selectedDestination 변경 시 자동으로 주차장 검색 실행.
  useEffect(() => {                   // selectedDestination 상태 변경시 실행.
    // 목적지가 설정되었고, currentLocationId가 설정된 경우(사업자의 주차장 등록 모드) 실행.
    if (selectedDestination && currentLocationId) {
          handleParkingSearch();      // 아래에 정의된 주변 주차장 검색 함수 호출.
    }
  }, [selectedDestination]);
  
  // --- useEffect 모음 종료. ---



  // --- 함수 정의. ---

  // MapView로부터 지도를 전달받아 상태 업데이트.
  const handleMapReady = useCallback((mapInstance, tmapv2Object) => {
    // useCallback : 함수를 메모리에 저장해두고 재사용. 리렌더링 시 재생성 방지.
    // mapInstance : 지도 객체. mapView에서 map으로 받아옴.
    // tmapv2Object : mapview에서 받아온 tmap객체.
    setMap(mapInstance);
  }, []);  

  // 기존 마커 제거.
  const removeMarkers = () => {
    // searchMarkers 배열의 marker 객체를 하나씩 반복하여 제거함.
    for (let marker of searchMarkers) {     
      marker.setMap(null);      // 값을 null로 변경. 지도 화면에서 마커를 시각적으로 제거.
    }

    // 배열 자체를 비움.
    setSearchMarkers([]);                   
  };

  // 현재 선택된 주차장과 목적지 정보 가져오는 메서드.
  const handleLogSubmit = async () => {
    // state의 존재 여부로 유효성 검사.
    // selectedParkingDetail : 특정 주차장 상세 정보.
    // selectedDestination : 선택한 목적지의 정보.
    if (!selectedParkingDetail || !selectedDestination) {
      alert("주차장 또는 목적지 정보가 없습니다.");
      return;
    }

    // 백엔드로 보낼 최종 요청 DTO.
    const logRequestDto = {
      parkingId: selectedParkingDetail.parkingId,           // 특정 주차장 ID.
      selectedDestinationName: selectedDestination.name     // 선택한 목적지명.
    };

    // 로그인 상태에 따라 헤더 다르게 설정.
    // 비로그인 시 빈 config, 로그인 시 토큰이 담긴 config.
    // 로그인 상태이지만 토큰이 없는 경우에도 빈 config.
    const config = {};
    if(isLoggedIn) {      // state로 로그인 상태 확인.
      const token = localStorage.getItem("token");
      if(token) {         // 토큰 여부 확인.
        config.headers = {Authorization : `Bearer ${token}`};
      }
    }

    try {
      // 백엔드의 주차장 선택 로그 저장 메서드 호출.
      await axios.post(`${API_BASE_URL}/api/parking/log`, logRequestDto, config);

      // 로그 저장 성공 후 콘솔 로그, 알림 생성.
      console.log("로그 전송 성공:", logRequestDto.parkingId);
      alert(`[${selectedParkingDetail.name}]\n주차장 선택 기록이 저장되었습니다.`);
    } catch(error) {
      // 실패 시 콘솔 로그, 알림 생성.
      console.error("로그 전송 실패:", error);
      alert("로그 저장에 실패했습니다.");
    }
  };
  

  // --- 지도 및 검색 관련 메서드. ---

  // 목적지용 단순 정보창. 마커에 띄우는 정보.
  const displayDestinationInfo = (marker, name, address) => {
    // 기존에 열려있던 마커 정보창 닫기.
    if (infoWindow) {               
      infoWindow.setMap(null);      // 값을 null로 설정하여 닫음.
    }

    // 정보창 외 리액트 모달이 열려있으면 닫음.
    setSelectedParkingDetail(null);

    // JS안에 HTML코드를 직접 넣은 것. 백틱(`)과 템플릿리터럴(${}) 사용.
    // 함수 전달인자로 받은 name, address 사용.
    // 정보창에 들어가게 HTML 구조.
    const content = `<div style="padding: 10px; font-size: 14px; background: white; border-radius: 4px; box-shadow: 0 2px 6px rgba(0,0,0,0.3);">
                       <strong>${name}</strong><br>
                       ${address}
                     </div>`;

    // 정보창 생성. Tmap 제공 기능. 
    const newInfoWindow = new window.Tmapv2.InfoWindow({
      position: marker.getPosition(),   // 마커 객체에 들어있는 postion 값 가져오는 함수.
      content: content,
      type: 2,                          // Tmap SDK에서 지정되어있는 정보창 형태. 1은 사각형 박스. 2는 말풍선.
      map: map                          // useState의 map객체.
    });

    setInfoWindow(newInfoWindow);       // 생성된 정보창 객체 저장. useState 객체.
  };

  // 검색 로직 시작점. Tmap API 호출.
  const handleSearch = () => {
    // 검색 전 유효성 검사.
    if (!map) {
      alert("지도가 아직 로드되지 않았습니다.");
      return;
    }
    if (!keyword) {
      alert("검색어를 입력해주세요.");
      return;
    }

    // 이전 검색 결과 초기화.
    removeMarkers();              // 기존 마커 제거.
    setPlaces([]);                // 검색 결과 목록 배열 초기화.
    if (infoWindow) {             // 열려 있던 정보 창을 제거.
      infoWindow.setMap(null);
    }

    // 검색 옵션.
    const optionObj = {
      count: 10,                  // 검색 결과 최대 개수.
      resCoordType: "WGS84GEO"    // 좌표계 타입.
    };

    // API 요청 파라미터. API요청 후 실행될 콜백 함수 모음.
    const params = {
      
      // 데이터 로드 완료 시 사용. res : Tmap API의 JSON 형태 반환 데이터.
      onComplete: (res) => {
        console.log("TMAP API 실제 응답:", res);
        // 검색 결과 담을 임시 배열 변수 선언.
        let poiItems = [];
        
        // POI목록 추출.
        const responseData = res._responseData;
        // 데이터 경로가 확실히 확인될 때만 마지막 값을 꺼내오도록 하여 오류 사전 차단.
        if (responseData && responseData.searchPoiInfo && responseData.searchPoiInfo.pois && responseData.searchPoiInfo.pois.poi) {
            poiItems = responseData.searchPoiInfo.pois.poi;               // responseData에서 poi라는 배열 전체를 가져와 poiItems 변수에 담음.
        } 
        
        // 검색 결과의 유무에 따른 로직.
        if (poiItems.length > 0) {
          setPlaces(poiItems);                                            // place 객체에 poiItems로 대체(저장).
          const bounds = new window.Tmapv2.LatLngBounds();                // LatLngBounds : 직사각형 영역을 정의, 관리하는 객체.
          let newMarkers = [];                                            // 검색 결과들의 마커를 담을 배열 선언.
          console.log("검색 결과 수:", poiItems.length);                  // 콘솔 로그 출력.
          
          // poiItems의 모든 요소를 순회하며 지도에 마커 생성, 클릭 이벤트 등록.
          poiItems.forEach((place, index) => {
            // 좌표값 추출.
            let placeLat = Number(place.noorLat) || Number(place.lat);    // 위도
            let placeLon = Number(place.noorLon) || Number(place.lon);    // 경도
            console.log(`[${index}번 항목] 좌표:`, placeLat, placeLon);   // 콘솔 로그 출력.

            // 좌표 유효성 검토.
            if (isNaN(placeLat) || isNaN(placeLon)) {
              console.error("잘못된 좌표 데이터:", place);                // 콘솔 에러 출력.
              return; 
            }

            // 위도, 경도 값으로 좌표 객체 생성.
            const point = new window.Tmapv2.LatLng(placeLat, placeLon);

            // 좌표 객체를 지도 경계 계산 객체에 포함.
            if(index < 6) {
              bounds.extend(point);                                       // bounds : 지도가 보여줘야 할 사각형 영역을 계산하는 객체.
            }
            
            // 마커 객체 생성, 초기 설정 지정.
            const marker = new window.Tmapv2.Marker({
              position: point,
              map: map,
              title: place.name
            });
            
            // 생성된 마커에 클릭이벤트 리스너 적용.
            window.Tmapv2.event.addListener(marker, 'click', () => {
              const address = place.newAddressList?.newAddress?.[0]?.fullAddressRoad || place.address || '주소 정보 없음';  // 도로명주소와 지번주소 찾아 address에 저장. 1순위 도로명주소, 2순위 지번주소.
              displayDestinationInfo(marker, place.name, address);        // displayDestinationInfo함수 호출.
            });

            // 검색 결과 마커를 담는 배열에 추가.
            newMarkers.push(marker);
          });

          // state객체에 마커 목록 저장, 관리.
          setSearchMarkers(newMarkers); 
          console.log("fitBounds 직전 bounds 객체:", bounds);             // 콘솔 로그 출력.
          // 지도 화면을 조정하여 모든 검색 결과가 보이게 함.
          map.fitBounds(bounds);                                          // fitBounds : Tmap SDK의 내장 함수. 지도의 중심과 확대 수준 결정.
        } else {                                                          // 검색 결과가 없는 경우 알림 출력. 
          alert("검색 결과가 없습니다.");
        }
      },

      // 검색 진행 중 사용.
      onProgress: () => {
        console.log("POI 검색 중...");            // 콘솔 로그 출력.
      },

      // 검색 실패 시 사용.
      onError: (error) => { 
        console.error("POI 검색 에러:", error);   // 콘솔 로그 출력.
        alert("검색 중 오류가 발생했습니다.");    // 알림 창 출력.
      }
    };
    
    // API 호출 실행.
    new window.Tmapv2.extension.TData().getPOIDataFromSearchJson(keyword, optionObj, params);
  };

  // 장소 클릭 이벤트 핸들러 정의.
  // 지도 화면을 해당 장소로 이동, 정보 창을 띄움.
  const handlePlaceClick = (place) => {
    // 유효성 검사. 지도 로드 확인.
    if (!map) return; 
  
    // 응답 필드명 재확인.
    let placeLat = Number(place.noorLat) || Number(place.lat);    // 위도.
    let placeLon = Number(place.noorLon) || Number(place.lon);    // 경도.

    // 좌표 유효성 확인.
    if (isNaN(placeLat) || isNaN(placeLon)) return;

    // 좌표 객체 생성.
    const point = new window.Tmapv2.LatLng(placeLat, placeLon);

    // 최종 선택 목적지 state에 저장.
    setSelectedDestination({
      name : place.name,
      address : place.newAddressList?.newAddress?.[0]?.fullAddressRoad || place.address || '주소 정보 없음',
      lat : placeLat,
      lon : placeLon,
    });

    // 검색 결과 목록 닫음.
    setPlaces([]);
    // 지도 중심 좌표 설정.
    map.setCenter(point); 
    // 지도 확대 수준 설정.
    map.setZoom(17);

    console.log("handlePlaceClick: 클릭한 장소 이름:", place.name);             // 콘솔 로그 출력.
    console.log("handlePlaceClick: 현재 searchMarkers 배열:", searchMarkers);   // 콘솔 로그 출력.

    // 검색 조건에 가장 먼저 일치하는 마커 객체를 저장할 상수. 
    // 배열을 반복 조회함. 조건에 일치하는 마커 객체를 targetMarker에 저장.
    const targetMarker = searchMarkers.find(m => 
      m.title === place.name
    );

    // 조건 일치 마커 존재시 지도 위에 정보 창 표시.
    if (targetMarker) {
      const address = place.newAddressList?.newAddress?.[0]?.fullAddressRoad || place.address || '';
      displayDestinationInfo(targetMarker, place.name, address);                // displayDestinationInfo 함수 실행. 마커 위에 정보창 띄움.
    }
  };

  // 목적지 주변 주차장 검색.
  const handleParkingSearch = () => {
    // 유효성 검사(1).
    // 지도 객체, 목적지 좌표 존재 여부.
    // 동작 순서 상 map 객체가 없으면 애초에 selectedDestination도 존재할 수 없는 구조임.
    if (!map || !selectedDestination) {
      alert("목적지가 선택되지 않았습니다.");                             // 알림 창 출력.
      return;
    }
    // 유효성 검사(2).
    // Tmap 검색 라이브러리(TData) 로드 확인.
    if (!window.Tmapv2 || !window.Tmapv2.extension || !window.Tmapv2.extension.TData) {
      alert("Tmap 검색 라이브러리(TData)가 로드되지 않았습니다.");      // 알림 창 출력.
      return;
    }

    // 로딩 상태(state) 시작으로 변경.
    setIsParkingLoading(true);

    // 이전 검색 결과인 목적지 마커, 목록 초기화.
    removeMarkers();              // 목적지 마커 제거.
    setParkingLots([]);           // 주차장 목록 초기화.
    if (infoWindow) {
      infoWindow.setMap(null);    // 마커의 정보창 닫기(지도에서 삭제).
      setInfoWindow(null);        // 마커의 정보창 닫기(변수를 비움).
    }

    // window.Tmapv2 객체를 지역 변수에 할당하여 코드 간소화.
    // 전역 변수로 할당하지 않는 이유 : 앱이 켜진 직후에는 window.Tmapv2가 없을수도 있음.
    const Tmapv2 = window.Tmapv2;

    // Tmap POI API 옵션 설정.
    const optionObj = {
      count: 20,                          // 검색 결과 최대 개수.
      resCoordType: "WGS84GEO",           // 응답 좌표계(위경도).
      centerLon: selectedDestination.lon, // 검색 중심 경도(목적지).
      centerLat: selectedDestination.lat, // 검색 중심 위도(목적지).
      radius: 2,                          // 검색 반경(2km).
      sort: 'distance'                    // 정렬 기준(거리순).
    };

    // API 요청 파라미터.
    const params = {
      // 데이터 로드 완료 시 사용. res : Tmap API의 JSON 형태 반환 데이터.
      onComplete: (res) => {
        // 검색 결과 담을 임시 배열 변수 선언.
        // 이후 이 데이터는 백엔드 서버로 전송됨.
        let tmapPoiItems = [];

        // POI 목록 추출.
        const responseData = res._responseData;
        // 데이터 경로가 확실히 확인될 때만 마지막 값을 꺼내오도록 하여 오류 사전 차단.
        if (responseData && responseData.searchPoiInfo && responseData.searchPoiInfo.pois && responseData.searchPoiInfo.pois.poi) {
            tmapPoiItems = responseData.searchPoiInfo.pois.poi;       // responseData에서 poi라는 배열 전체를 가져와 tmapPoiItems 변수에 담음.
        } 
        
        // 검색 결과의 유무에 따른 로직.
        if (tmapPoiItems.length > 0) {
          // 백엔드 추천 API 호출
          fetchRecommendations(tmapPoiItems);                         // fetchRecommendations 함수 호출. 백엔드 API 호출임.
        } else {
          alert("주변 2km 이내에 검색된 주차장이 없습니다.");         // 알림 창 출력.
        }
      },

      // 검색 실패 시 사용.
      onError: (error) => { 
        console.error("Tmap 주차장 1차 검색 에러:", error);           // 콘솔 에러 출력.
        alert("Tmap 검색 중 오류가 발생했습니다.");                   // 알림 창 출력.
      }
    };
    
    // 목적지 주변 주차장 검색 API 호출 실행(검색어: "주차장").
    new Tmapv2.extension.TData().getPOIDataFromSearchJson("주차장", optionObj, params);
  };

  // 백엔드의 주차장 추천 API를 호출 함수.
  const fetchRecommendations = async (tmapPoiItems) => {
    // Tmap 원본 데이터 -> 백엔드 DTO 형식 변환.
    // map은 JS의 배열 메서드.
    // tmapPoiItems에 들어 있는 원본 place 객체에서 데이터를 추출하여 tmapParkingList 라는 새로운 객체에 저장.
    const tmapParkingList = tmapPoiItems.map(place => ({
      tmapPkey: place.pkey,
      name: place.name,
      // 옵셔널 체이닝(?.) 사용으로 값이 없으면 에러 대신 undefined를 반환하고 넘어감.
      // 1순위 : 도로명주소(newAddressLise(경로), newAddress(배열명), fullAddressRoad(도로명 주소 문자열)), 2순위 : 지번 주소(address), 3순위 : 주소 정보 없음.
      address: place.newAddressList?.newAddress?.[0]?.fullAddressRoad || place.address || '주소 정보 없음',
      latitude: Number(place.noorLat) || Number(place.lat),
      longitude: Number(place.noorLon) || Number(place.lon),
      distance: Number(place.radius)
    }));

    // 백엔드로 보낼 최종 요청 DTO.
    const requestDto = {
      tmapParkingList: tmapParkingList,
      filter: filterOptions,      // 현재 state의 필터 옵션
      destinationLocationId: selectedDestination.locationId || null,
      destinationLat: selectedDestination.lat,
      destinationLon: selectedDestination.lon
    };

    try {
      // 백엔드의 주차장 추천 API 호출.
      // /api/parking/recommend
      // await : axios.post()로 백엔드에 요청을 보내고, 응답이 도착할 때 까지 다음 라인으로 넘어가지 않고 기다림.
      // await와 async는 세트.
      // 백틱(`)은 JS의 템플릿 리터럴.
      const response = await axios.post(`${API_BASE_URL}/api/parking/recommend`, requestDto);

      // 백엔드에서 정렬된 IntegratedParking 목록
      // response에는 헤더, 상태 코드등 담겨있고, 그 중 body역할인 data 가져옴.
      const recommendedList = response.data;

      if (recommendedList.length > 0) {
        // Tmap 라이브러리를 변수에 할당.
        const Tmapv2 = window.Tmapv2;
        // 지도 화면의 크기를 결정할 빈 사각형 틀 생성.
        const bounds = new Tmapv2.LatLngBounds();
        // 마커 객체들을 담아두기 위한 빈 배열 객체 생성.
        let newMarkers = [];
        // 백엔드에서 받은 최종 목록을 state에 저장.
        setParkingLots(recommendedList);

        // recommendedList : 배열 객체.
        // place : 배열 내 각각의 객체 지칭하는 이름.(변경 가능)
        recommendedList.forEach(place => {
          // 백엔드에서 응답받은 데이터를 사용해 좌표 객체 생성.
          const point = new Tmapv2.LatLng(place.latitude, place.longitude);
          // 지도에 표시할 영역 확장.
          bounds.extend(point);
          // 마커 객체 생성.
          const marker = new Tmapv2.Marker({
            position: point,
            map: map,       // state의 map.
            title: place.name
          });

          // 마커 클릭 시 'selectedParkingDetail' state 업데이트
          // addListener : Tmap 라이브러리의 메서드.
          Tmapv2.event.addListener(marker, 'click', () => {
            console.log("주차장 마커 클릭됨!", place.name);

            // tmap 팝업이 열려있으면 닫음.
            if (infoWindow) {
              // 지도 화면의 팝업 제거.
              infoWindow.setMap(null);
              // state값 제거.
              setInfoWindow(null);
            }
            // 상세 정보 호출 API 메서드.
            handleShowParkingDetails(place);
          });
          // 만들어둔 마커 객체 배열에 추가.
          newMarkers.push(marker);
        });
        // 완성된 마커 객체 배열을 state에 저장.
        setSearchMarkers(newMarkers);
        // 마커들이 나올 수 있도록 지도 확대 정도 조절.
        map.fitBounds(bounds);
      } else {
        // 검색 결과가 없을 경우. state에 빈 배열 객체 저장.
        setParkingLots([]);
        alert("필터 조건에 맞는 주차장이 없습니다.");
      }
    } catch (error) {     // try에서 에러 발생시 실행.
      // API 호출 에러 발생한 경우.
      console.error("백엔드 추천 API 호출 에러:", error);
      alert("주차장 추천 목록을 가져오는 데 실패했습니다.");
    } finally {           // 마지막에 무조건 실행.
      // 로딩 종료. 로딩 상태 나타내는 state값 변경.
      setIsParkingLoading(false);
    }
  };

  // 상세 정보 조회, 모달창 표시.
  const handleParkingLotClick = async (place) => {
    // async : 비동기 함수.
    // 기존 팝업 정리(tmap 팝업 객체).
    if (infoWindow) {
        infoWindow.setMap(null);
        setInfoWindow(null);
    }
      // 상세 정보 호출 API 메서드. 상세 정보 모달창 표시.
      handleShowParkingDetails(place);
  };

  // 상세 정보 표시 메서드.
  const handleShowParkingDetails = async (placeFromList) => {
    
    // 모달 띄우기.
    // placeFromList : /recommend API가 반환한 ParkingResponseDto.
    setSelectedParkingDetail(placeFromList);        // 주차장의 상세 정보 state 설정.

    // 주차장 상세 정보 모달 내부에 로딩 상태 표시.
    setIsDetailLoading(true);                       // 로딩 상태 state 설정.

    try {
      // 백엔드의 주차장 상세 정보 요청 API 호출.
      const response = await axios.get(`${API_BASE_URL}/api/parking/details/${placeFromList.parkingId}`);

      // API 요청 성공 시.
      // selectedParkingDetail state 설정(업데이트).
      // 기본 정보(placeFromList)에 주차장 상세 정보 요청 API에서 받은 상세 정보를 덮어씀.
      setSelectedParkingDetail(prevDetails => ({
        // ... : JS의 전개 연산자. 기존 내용을 넣음.
        // 기본 정보 넣기.
        ...prevDetails,           // distance 등 기존 /recommend 정보 유지.
        // 상세 정보 넣기.
        ...response.data          // priceInfo, operatingHours 등 /details 정보 덮어쓰기.
      }));
    } catch (error) {             // API 요청 실패 시.
      console.error("상세 정보 로딩 실패:", error);
      alert("주차장의 상세 정보를 불러오는 데 실패했습니다.");
    } finally {                   // 마지막에 무조건 실행.
      // 로딩 종료. 로딩 상태 나타내는 state 설정.
      setIsDetailLoading(false);
    }
  };


  // --- 지도 및 검색 관련 메서드 종료. ---


  // --- 인증 관련 메서드. ---

  // 유저 인증 폼의 입력 필드 초기화 메서드.
  const clearFormStates = () => {
    setUsername('');
    setPassword('');
    setEmail('');
    setUserType('일반회원');
  };

  // 로그인 모달 창 여는 메서드.
  const openModal = () => {
    setIsModalOpen(true);       // 모달 창 열기.
    setAuthMode('login');       // Auth 모드 로그인으로 설정.
    clearFormStates();          // 폼 입력 필드 초기화 메서드 호출.
  };

  // 로그인 모달 창 닫는 메서드.
  const closeModal = () => {
    setIsModalOpen(false);      // 모달 창 닫기.
    clearFormStates();          // 폼 입력 필드 초기화 메서드 호출.
  };

  // 로그인 / 회원가입 모드 변경 메서드.
  const switchAuthMode = (mode) => {
    setAuthMode(mode);          // Auth 모드 변경.
    clearFormStates();          // 폼 입력 필드 초기화 메서드 호출.
  };

  // 로그인 폼 제출 시 실행되는 메서드.
  // e(이벤트 객체)를 받아 비동기 작업 수행.
  const handleLoginSubmit = async (e) => {
    // 폼 제출(form 태그의 submit 타입의 버튼 클릭) 시 페이지 새로고침 동작을 막음.
    e.preventDefault();

    // 백엔드로 보낼 최종 요청 DTO.
    const requestDto = {
      userId: username,
      password: password
    };    

    try {
      // 백엔드의 로그인 API 호출.
      const response = await axios.post(`${API_BASE_URL}/api/auth/login`, requestDto);

      if (response.data.success) {          // 로그인 성공 시.
        // 토큰 설정.
        const loginToken = response.data.token;     // 토큰 추출.
        localStorage.setItem("token", loginToken);  // localStorage에 토큰 값 저장.
        setIsLoggedIn(true);                        // 로그인 상태 state 설정.

        // 역할 설정.
        const loginRole = response.data.role;       // 역할 추출.
        localStorage.setItem("role", loginRole);    // localStorage에 역할 값 저장.
        setUserRole(loginRole);                     // 유저 구분 state 설정.
        
        alert("로그인에 성공했습니다.");            // 알림 생성.
        closeModal();                               // 로그인 모달 창 닫기.

        // 사업자 유저의 로그인인 경우 목록을 불러옴.
        // useEffect를 통해 지도 바로 이동.
        // 모달창을 열어 목록을 보여주진 않음.
        if (loginRole === 'ROLE_BUSINESS') {
          // 사업장 목록을 가져오는 메서드 호출.
          fetchMyLocations(loginToken);
        }
      }
    } catch (error) {                       // 로그인 실패 시.
      if (error.response && error.response.data && error.response.data.message) {     // 로그인 실패의 구체적인 원인 제공.
        alert(error.response.data.message);
      } else {                                                                        // 예상치 못한 에러에 대한 안전장치.
        alert("로그인 중 오류가 발생했습니다.");
      }
    }
  };

  // 회원가입 폼 제출 시 실행되는 핸들러 메서드.
  // e(이벤트 객체)를 받아 비동기 작업 수행.
  const handleSignupSubmit = async (e) => {
    // 폼 제출 시 페이지 새로고침(submit) 동작을 막음
    e.preventDefault();

    // 삼항 연산자 사용.
    // UI에 표시되는 옵션을 DB 데이터로 변경.
    const roleToSend = userType === '일반회원' ? 'ROLE_USER' : 'ROLE_BUSINESS'; 

    // 백엔드로 보낼 최종 요청 DTO.
    const signupRequestDto = {
      userId: username,
      password: password,
      email: email,
      role: roleToSend
    };

    try {
      // 백엔드의 회원 가입 API 호출.
      const response = await axios.post(`${API_BASE_URL}/api/auth/register`, signupRequestDto);

      if (response.data.success) {                                                  // 회원가입 성공 시.
        alert("회원가입이 완료되었습니다. 로그인해주세요.");                        // 알림 생성.
        switchAuthMode('login');                                                    // 로그인/회원가입 모드 변경 메서드 호출.
      }
    } catch (error) {                                                               // 회원가입 실패 시.
      if (error.response && error.response.data && error.response.data.message) {   // 회원가입 실패의 구체적인 원인 제공.
        alert(error.response.data.message);
      } else {
        alert("회원가입 중 오류가 발생했습니다.");                                  // 예상치 못한 에러에 대한 안전장치.
      }
    }
  };

  // 로그아웃 메서드.
  const handleLogout = () => {
    // 토큰 설정.
    localStorage.removeItem("token");     // localStorage에서 토큰 값 삭제.
    setIsLoggedIn(false);                 // 로그인 상태 state 설정.
    
    // 역할 설정.
    localStorage.removeItem("role");      // localStorage에서 역할 값 삭제.
    setUserRole(null);                    // 유저 구분 state 설정.
    
    // 지도 위 마커 삭제.
    removeMarkers();

    // state 설정.
    setCurrentLocationId(null);           // 최근 수정, 등록 사업장 ID state 설정.
    setMyLocations([]);                   // 사업장 목록 state 설정.
    setSelectedDestination(null);         // 최종 선택 목적지 state 설정.
    setParkingLots([]);                   // 주차장 검색 결과 목록 state 설정.


    alert("로그아웃 되었습니다.");
  };

  // --- 인증 관련 메서드 종료. ---


  // --- 사업자 기능 관련 메서드. ---

  // 사업자 유저 기능.
  // 사업자 마이페이지 모달 여는 메서드.
  const openBusinessModal = () => {
    setIsBusinessModalOpen(true);
  };

  // 사업자 유저 기능.
  // 내 사업장 목록을 가져오는 메서드.
  const fetchMyLocations = async (token) => {
    // async : 비동기 함수.
    // 매개변수로 token. 인증정보 가져옴.

    // 보안 헤더 설정.
    const config = {
    // 로그인 시 발급받은 토큰을 함께 전송.
    // Authorization : 권한이 있음을 알리는 표준 Key.
      headers: { Authorization: `Bearer ${token}` } 
    };
    try {
      // 백엔드의 내 주차장 목록 불러오는 API 호출.
      const locationResponse = await axios.get(`${API_BASE_URL}/api/business/location`, config);

      // 로딩 성공 시 state에 저장.
      setMyLocations(locationResponse.data);
    } catch (error) {
      // 로딩 실패 시 실행.
      console.error("사업장 목록 로딩 실패:", error);
    }
  };

  // 사업자 유저 기능.
  // 장소 검색 후 해당 장소를 사업장으로 등록하는 메서드.
  const handleRegisterLocation = async () => {
    // async : 비동기 함수.
    // 목적지(장소)가 선택되었는지 state로 확인.
    if (!selectedDestination) {
      alert("먼저 장소를 검색하고 선택해야 합니다.");
      return;
    }
    // 로그인 상태, 사업자 유저인지 state로 확인.
    if (!isLoggedIn || userRole !== 'ROLE_BUSINESS') {
      alert("사업자 회원으로 로그인해야 등록할 수 있습니다.");
      return;
    }
    
    // localStorage에서 토큰 가져옴.
    const token = localStorage.getItem("token");

    // 백엔드로 보낼 최종 요청 DTO.
    // selectedDestination : 장소 이름, 주소 좌표 정보 저장되어있는 state.
    const requestDto = {
      locationName: selectedDestination.name,
      address: selectedDestination.address,
      latitude: selectedDestination.lat,
      longitude: selectedDestination.lon
    };

    // 보안 헤더 설정.
    const config = {
      // 로그인 시 발급받은 토큰을 함께 전송.
      // Authorization : 권한이 있음을 알리는 표준 Key.
      headers : { Authorization: `Bearer ${token}` }
    }

    try {
      // 백엔드의 사업장 등록 API 호출.
      await axios.post(`${API_BASE_URL}/api/business/location`, requestDto, config);
      
      // 등록 성공 후 알림 생성.
      alert(`'${selectedDestination.name}'이(가) 내 사업장으로 등록되었습니다.`);
      
      // 등록 성공 후 목적지 선택 초기화.
      setSelectedDestination(null);             // 목적지(장소) 초기화.
      removeMarkers();                          // 지도 마커 제거.
    } catch (error) {
      // 등록 실패시 실행.
      console.error("사업장 등록 실패 :", error);
      alert("사업장 등록에 실패했습니다. 이미 등록된 장소일 수 있습니다.");
    }
  };

  // 사업자 유저 기능.
  // 사업자가 자신의 사업장을 클릭 시 실행되는 메서드.
  // 가게 위치를 지도의 목적지로 설정하고, 등록 모드 실행, 기존 등록된 추천 주차장 목록 불러옴.
  const fetchParkingsForLocation = async (locationId) => {
    // 클릭한 사업장 정보를 찾음.
    // myLocatinos : 해당 사업자가 등록한 모든 사업장 목록 state.
    const selectedBizLocation = myLocations.find(data => data.id === locationId);

    // 유효성 검사.
    if (!selectedBizLocation) {
      alert("선택된 사업장 정보를 찾을 수 없습니다.");
      return;
    }

    // 지금 등록하려는 사업장 ID를 state로 설정.
    setCurrentLocationId(locationId);

    // 선택된 사업장을 목적지 state로 설정. 
    setSelectedDestination({
      name: selectedBizLocation.locationName,
      address: selectedBizLocation.address,
      lat: selectedBizLocation.latitude,
      lon: selectedBizLocation.longitude
    });

    const token = localStorage.getItem("token");

    // 보안 헤더 설정.
    const config = { 
      // 로그인 시 발급받은 토큰을 함께 전송.
      // Authorization : 권한이 있음을 알리는 표준 Key.
      headers: { Authorization: `Bearer ${token}` } 
    };

    try {
      // 백엔드에서 이미 등록된 주차장 목록 불러오는 API 호출.
      const response = await axios.get(`${API_BASE_URL}/api/business/location/${locationId}/parking`, config);

      // API응답을 state에 저장.
      setMyLocationParkings(response.data);
    } catch (error) {
      // 요청 실패 시 실행.
      console.error("추천 주차장 목록 로딩 실패:", error);
      setMyLocationParkings([]); // state 배열 값 삭제.
    }
  };

  // 사업자 유저 기능.
  // 사업자 마이페이지 모달 내 주소 검색 핸들러 메서드.
  const handleManualSearch = () => {
    // 유효성 검사.
    if (!map) { 
      alert("지도가 아직 로드되지 않았습니다.");
      return;
    }
    // 검색 버튼에 연결되어 있어 검색어 존재 여부 유효성 검사.
    // onChange()로 검색창에 타이핑하는 실시간으로 manualSearchKeyword state에 값이 들어감.
    if (!manualSearchKeyword) {
      alert("검색어를 입력해주세요.");
      return;
    }

    // 검색 옵션.
    const optionObj = {
      count: 5,                   // 검색 결과 최대 개수.
      resCoordType: "WGS84GEO"    // 좌표계 타입.
    };

    // API 요청 파라미터. API 요청 후 실행될 콜백 함수 모음.
    const params = {
      // 데이터 로드 완료 시 사용. res : Tmap API의 JSON 형태 반환 데이터.
      onComplete: (res) => {
        // 검색 결과 담을 임시 배열 변수 선언.
        let poiItems = [];

        // POI목록 추출.
        const responseData = res._responseData;
        // 데이터 경로가 확실히 확인될 때만 마지막 값을 꺼내오도록 하여 오류 사전 차단.
        if (responseData && responseData.searchPoiInfo && responseData.searchPoiInfo.pois && responseData.searchPoiInfo.pois.poi) {
            poiItems = responseData.searchPoiInfo.pois.poi;         // responseData에서 poi라는 배열 전체를 가져와 poiItems 변수에 담음.
        } 
        
        // 검색 결과의 유무에 따른 로직.
        if (poiItems.length > 0) {
          setManualSearchResults(poiItems);         // manualSearchResults State에 저장.
        } else {
          alert("검색 결과가 없습니다.");
          setManualSearchResults([]);               // 검색 결과 없는 경우 manualSearchResults State 비움.
        }
      },

      // 검색 실패 시 사용.
      // onProgress는 필수가 아닌 선택 사항이기 때문에 가볍게 동작하는 모달 검색에서는 생략됨.
      onError: (error) => { 
        console.error("모달 POI 검색 에러:", error);        // 콘솔 로그 출력.
        alert("검색 중 오류가 발생했습니다.");              // 알림 창 출력.
      }
    };
    
    // API 호출 실행.
    new window.Tmapv2.extension.TData().getPOIDataFromSearchJson(manualSearchKeyword, optionObj, params);
  };

  // 사업자 유저 기능.
  // 모달 내 검색 결과 클릭 핸들러 메서드.
  const handleManualPlaceSelect = (place) => {
    // state 설정.
    setManualSelectedPlace(place);          // 결과 중 수동으로 등록하기 위해 선택한 장소 정보 설정.
    setManualSearchResults([]);             // 검색 결과 목록 state 비우기.
    setManualSearchKeyword("");             // 검색 키워드 state 비우기.
    setManualParkingName("");               // 등록용 주차장 이름 state 비우기.
    setNewParkingInfo("");                  // 추가 정보 입력창 state 비우기.
  };

  // 모달 내 최종 추천 버튼 클릭 핸들러.
  const handleFinalManualRegister = async (e) => {
    // 폼 제출 페이지 새로고침(submit) 동작을 막음.
    e.preventDefault();

    // 유효성 검사.
    if (!currentLocationId || !manualSelectedPlace || !manualParkingName) {
      alert("필수 정보가 누락되었습니다.");
      return;
    }

    const token = localStorage.getItem("token");

    // 백엔드로 보낼 최종 요청 DTO.
    const manualRegisterRequestDto = {
      parkingName: manualParkingName, // 사용자가 입력한 주차장 이름
      // Tmap 검색 주소.
      address: manualSelectedPlace.newAddressList?.newAddress?.[0]?.fullAddressRoad || manualSelectedPlace.address || '주소 정보 없음',
      // Tmap 검색 좌표.
      latitude: Number(manualSelectedPlace.noorLat) || Number(manualSelectedPlace.lat),
      longitude: Number(manualSelectedPlace.noorLon) || Number(manualSelectedPlace.lon),
      // Tmap의 PK.
      tmapPkey: manualSelectedPlace.pkey || null, 
      // 사용자 입력 추가 제공 정보.
      additionalText: newParkingInfo 
    };
    
    // 보안 헤더 설정.
    const config = {
      // 로그인 시 발급받은 토큰을 함께 전송.
      // Authorization : 권한이 있음을 알리는 표준 Key.
      headers: { Authorization: `Bearer ${token}` }
    };

    try {
      // 백엔드의 추천 주차장 수동 등록 API 호출.
      await axios.post(`${API_BASE_URL}/api/business/location/${currentLocationId}/parking-manual`, manualRegisterRequestDto, config);

      // 등록 성공 후 알림 생성.
      alert(`'${manualParkingName}'이(가) 추천 주차장으로 등록되었습니다.`);

      // 등록 성공 후 모달 상태 초기화.
      setManualSelectedPlace(null);       // 수동으로 등록하기 위해 선택한 주차장 state 초기화.
      setManualParkingName("");           // 등록용 주차장 이름 state 초기화.
      setNewParkingInfo("");              // 주차장 등록 시 입력하는 추가 정보 텍스트 state 초기화.
      setIsBusinessModalOpen(false);      // 사업자 마이페이지 모달 닫기. (state false 설정)
    } catch (error) {                     // 등록 실패 시.
      console.error("수동 주차장 등록 실패:", error);
      alert("주차장 등록에 실패했습니다.");
    }
  };

  // 마이페이지 주차장 목록의 항목(parking) 클릭 시, 상세/수정 모달 여는 메서드.
  const handleOpenMyParkingModal = (parking) => {
    setMyPageModalDetail(parking);                          // 클릭한 주차장 정보 state에 저장.
    setMyPageModalText(parking.additionalText || "");       // 기존 추가 정보를 텍스트 state에 저장
  };

  // 사업자 유저 기능.
  // 마이페이지 상세/수정 모달을 닫는 메서드.
  const handleCloseMyParkingModal = () => {
    setMyPageModalDetail(null);       // state 설정.
    setMyPageModalText("");           // state 설정.
  };

  // 사업자 유저 기능.
  // 수정 완료 버튼 클릭 시 동작하는 메서드.
  const handleUpdateMyParkingInfo = async () => {
    const token = localStorage.getItem("token");

    // 유효성 검사.
    // myPageModalDetail : 현재 수정 중인 주차장 정보 state.
    if (!token || !currentLocationId || !myPageModalDetail) {
      alert("오류가 발생했습니다. 다시 시도해주세요.");
      return;
    }

    // 백엔드로 보낼 최종 요청 DTO.
    const updateDto = {
      additionalText: myPageModalText         // state에 저장된 모달 안의 추가 정보 입력값.
    };

    // 보안 헤더 설정.
    const config = {
      // 토큰을 함께 전송.
      headers: { Authorization: `Bearer ${token}` }
    };

    try {
      // 백엔드의 추천 주차장 추가 정보 수정 API 호출.
      await axios.put(`${API_BASE_URL}/api/business/location/${currentLocationId}/parking-info/${myPageModalDetail.parkingId}`, updateDto, config);
      
      // 수정 성공 시 myLocationParkings state 설정(업데이트).
      // 배열 형태 state 내 특정 항목을 하나만 선택하여 수정하는 패턴.
      setMyLocationParkings(prevParkingList => 
        // map : 배열을 가공해 반복한 결과를 모아 새로운 배열을 반환함.
        prevParkingList.map(prevParking => 
          // 삼항 연산자 사용.
          // ...prevParking : JS의 전개 연산자. 기존 내용을 넣음.
          // 기존 내용을 넣은 뒤 additionalText만 변경하게 됨.
          prevParking.id === myPageModalDetail.id ? { ...prevParking, additionalText: myPageModalText } : prevParking
        )
      );

      handleCloseMyParkingModal();            // 상세 정보 모달 닫기.
      alert("정보가 수정되었습니다.");
    } catch (error) {                         // 수정 실패 시.
      console.error("추가 정보 업데이트 실패:", error);
      alert("정보 수정에 실패했습니다.");
    }
  };

  // 사업자 유저 기능.
  // 추천 주차장 연결 삭제 메서드.
  // 주차장 자체를 삭제하는게 아닌 사업장과 추천 주차장의 연결을 삭제하는 것.
  const handleDeleteMyParking = async () => {
    const token = localStorage.getItem("token");

    // 유효성 검사.
    // myPageModalDetail : 현재 삭제하려는 주차장 정보 state.
    if (!token || !currentLocationId || !myPageModalDetail) {
      alert("오류가 발생했습니다. 다시 시도해주세요.");
      return;
    }

    // 유저 재확인.
    // 브라우저 확인 및 취소 대화상자 생성.
    // 취소 클릭 시 메서드 종료.
    if (!window.confirm(`[${myPageModalDetail.parkingName}]\n\n이 주차장을 추천 목록에서 삭제하시겠습니까?`)) {
      return;
    }

    // 보안 헤더 설정.
    const config = {
      headers: { Authorization: `Bearer ${token}`}
    };

    try {
      // 백엔드의 추천 주차장 연결 삭제 API 호출.
      // businessParkingId에 myPageModalDetail.id 입력.
      await axios.delete(`${API_BASE_URL}/api/business/parking/${myPageModalDetail.id}`,config);

      // API 요청 성공 시 MyLocationParkings state 설정(업데이트).
      setMyLocationParkings(prevParkingList => 
        // filter : true인 항목만 남기고 false인 항목은 버려 새로운 배열을 반환함.
        // 연결을 삭제하고자 했던 항목만 삭제하고 새로운 배열 반환.
        prevParkingList.filter(prevParking => prevParking.id !== myPageModalDetail.id)
      );

      handleCloseMyParkingModal();      // 상세 정보 모달 닫기.
      alert("삭제되었습니다.");

    } catch (error) {                   // 수정 실패 시.
      console.error("추천 주차장 삭제 실패:", error);
      alert("삭제에 실패했습니다.");
    }
  };

  // 사업자 유저 기능.
  // 사업장 삭제 메서드.
  const handleDeleteLocation = async (locationId, locationName) => {
    const token = localStorage.getItem("token");
    // 유효성 검사. 로그인 확인.
    if (!token) {
      alert("오류: 다시 로그인해주세요.");
      return;
    }

    // 유저 재확인.
    // 브라우저 확인 및 취소 대화상자 생성.
    // 취소 클릭 시 메서드 종료.
    if (!window.confirm(`[${locationName}]\n\n이 사업장을 목록에서 삭제하시겠습니까?\n(등록된 추천 주차장 목록도 함께 삭제됩니다.)`)) {
      return;
    }

    // 보안 헤더 설정
    const config = {
      headers: { Authorization: `Bearer ${token}` }
    };

    try {
      // 백엔드의 사업장 삭제 API 호출.
      // 등록된 추천 주차장은 DB의 연쇄 삭제.
      await axios.delete(`${API_BASE_URL}/api/business/location/${locationId}`,config);

      // API 요청 성공 시 myLocations state 설정(업데이트).
      setMyLocations(locationList => 
        // filter : true인 항목만 남기고 false인 항목은 버려 새로운 배열을 반환함.
        locationList.filter(prevLocation => prevLocation.id !== locationId)
      );
      
      // 삭제한 사업장이 현재 선택된 사업장인 경우 폼 초기화.
      if (currentLocationId === locationId) {
        setCurrentLocationId(null);           // state 설정.
      }

      alert("사업장이 삭제되었습니다.");

    } catch (error) {                         // API 요청 실패 시.
      console.error("사업장 삭제 실패:", error);
      alert("사업장 삭제에 실패했습니다.");
    }
  };

  // 사업자 유저 기능.
  // 선택한 주차장을 자신 사업장의 추천 주차장으로 최종 등록하는 메서드.
  const handleFinalRegisterParking = async (place, locationId) => {
    // async : 비동기 함수.
    // 매개변수로 plcae : 등록할 주차장 객체, locationID 추천 주차장 등록될 사업장 ID.
    const token = localStorage.getItem("token");

    // 입력된 추가 정보 텍스트(newParkingInfo)를 가져옴. (state)
    const additionalText = newParkingInfo; 

      // 백엔드로 보낼 최종 요청 DTO.
    const registerRequestDto = {
      parkingId: place.parkingId,         // 추천 주차장으로 등록할 주차장의 ID.
      additionalText: additionalText      // 등록할 추가 정보.
    };

    // 보안 헤더 설정.
    const config = {
      // 로그인 시 발급받은 토큰을 함께 전송.
      // Authorization : 권한이 있음을 알리는 표준 Key.
      headers : { Authorization: `Bearer ${token}` }
    };

    try {
      // 백엔드의 추천 주차장 등록 API 호출.
      await axios.post(`${API_BASE_URL}/api/business/location/${locationId}/parking`, registerRequestDto, config);
      
      // axios의 특성과 try-catch의 흐름으로 if-else를 쓸 필요 없음.
      // axios는 서버에서 에러 응답이 오면 그 즉시 에러를 발생시킴(catch문 실행됨).
      alert(`'${place.name}'이(가) 사업장 추천 주차장으로 등록되었습니다.`);
      
      // 등록 성공 후 상태 초기화.
      setSelectedDestination(null);       // 목적지(사업장) 초기화.
      setParkingLots([]);                 // 주차장 검색 목록 초기화.
      removeMarkers();                    // 지도 마커 제거.
      setCurrentLocationId(null);         // 선택된 사업장 ID 초기화.
    } catch (error) {
      // 등록 실패시 실행.
      console.error("추천 주차장 최종 등록 실패:", error);
      alert("추천 주차장 등록에 실패했습니다.");
    }
  };

  // --- 사업자 기능 관련 메서드 종료. ---


  // --- 확인 필요 메서드들. ---

  // ======== 구버전 코드. 추후 수정 ========
  // 주차장 정보 마커 정보창.
  // 현재 사용되지 않음. 대체 함수 확인 필요.
  const displayParkingInfo = (marker, place) => {
    // 기존에 열려있던 마커 정보창 닫기.
    if (infoWindow) {
      infoWindow.setMap(null);
    }

    // 상수로 선언되어도 되는 이유. -> 함수가 실행될 때마다 새로 만들어지므로 let을 쓰지 않아도 괜찮음.
    // 거리를 km, m단위 가공.
    const distanceText = (place.distance >= 1)                          // 1km이상인지 확인. distance 는 km단위.
                         ? `${place.distance.toFixed(1)} km`            // tofixed : JS의 Number 객체에 내장된 메서드.
                         : `${(place.distance * 1000).toFixed(0)} m`;   // 지정 소수점까지만 남기고 반올림 후 String으로 반환.

    // 주차장 추천 여부에 따른 텍스트 표시 결정.
    const recommendText = place.isRecommended 
                          ? '<span style="color: orange; margin-right: 5px;">★추천</span>' 
                          : '';

    // 추가 제공 정보 있을 시 표시 결정.
    const additionalInfo = place.additionalText 
                           ? `<div style="font-size: 13px; color: #28a745; font-weight: bold; margin-top: 8px; padding: 5px; background-color: #f8f9fa; border-radius: 4px;">
                                ${place.additionalText}
                              </div>`
                           : '';

    // 주차장 선택 횟수 데이터가 없을 경우.
    const selectionCount = place.selectionCount ?? 0;   // 널 병함 연산자. place.selectionCount가 null or undefined인 경우 0 사용.


    // 정보창에 들어가게 HTML 구조.
    const content = `
      <div style="padding: 10px; font-size: 14px; background: white; border-radius: 4px; box-shadow: 0 2px 6px rgba(0,0,0,0.3); width: 250px; font-family: system-ui, sans-serif;">
        <strong style="font-size: 16px; font-weight: bold; display: block; margin-bottom: 3px;">
          ${recommendText} ${place.name}
        </strong>
        <span style="font-size: 13px; color: #555; display: block; margin-bottom: 8px;">
          ${place.address}
        </span>
        <div style="display: flex; justify-content: space-between; font-size: 13px; margin-top: 8px; padding-top: 8px; border-top: 1px dashed #ddd;">
          <span style="color: #007bff; font-weight: bold;">
            목적지로부터 ${distanceText}
          </span>
          <span style="color: #6c757d;">
            (선택 ${selectionCount} 회)
          </span>
        </div>
        ${additionalInfo}
      </div>
    `;

    // 정보창 생성. Tmap 제공 기능. 
    const newInfoWindow = new window.Tmapv2.InfoWindow({
      position: marker.getPosition(),   // 마커 객체에 들어있는 postion 값 가져오는 함수.
      content: content,
      type: 2,                          // Tmap SDK에서 지정되어있는 정보창 형태. 1은 사각형 박스. 2는 말풍선.
      map: map                          // useState의 map객체.
    });

    setInfoWindow(newInfoWindow);       // 생성된 정보창 객체 저장. useState 객체.
  }
  
  // ======== 구버전 코드. 추후 수정 ========
  // 구버전 코드인 handleRegisterParkingByAddress 메서드에서만 사용중.
  // 주소 문자열을 시/군/구/동/번지로 분리하는 메서드.
  // 학습용 코드이므로 예외 생기는 경우 넘어감. 추가 작업 필요하긴 함.
  const parseAddress = (address) => {
      // 띄어쓰기(공백이 하나 이상인경우)를 기준으로 분리.
      // /.../ : JS에서 정규표현식을 감싸줄 때 사용하는 기호.
      const parts = address.split(/\s+/);
      
      // 유효성 검사로 최소한 시, 구, 동은 있다고 가정함.
      if (parts.length < 3) {
          return null;
      }
      
      // 첫 번째 요소. 시 / 도.
      const city_do = parts[0];
      
      // 두 번째 요소. 구 / 군.
      const gu_gun = parts[1];
      
      // 세 번째 요소. 등 / 읍 / 면.
      // 동/읍/면 (세 번째 요소. 그 이후는 번지 등으로 간주)
      const dong = parts[2];
      
      // 나머지 요소. 번지 등 세부 주소.
      // 나머지 요소는 공백을 기준으로 다시 합침.
      const bunji = parts.slice(3).join(' '); 
      
      //
      return { city_do, gu_gun, dong, bunji };
  };

  // ======== 구버전 코드. 추후 수정 ========
  // 사업자 유저 기능.
  // 특정 사업장에 추천 주차장 등록하기
  const handleRegisterParkingByAddress = async (e) => {
    e.preventDefault();
    console.log("1. 함수 시작"); // ★★★ 1
      
    if (!currentLocationId) {
        console.warn("2. 사업장 ID 없음"); // ★★★ 2
        alert("먼저 사업장을 선택해주세요.");
        return;
    }
    if (!newParkingAddress) {
        console.warn("3. 주소 입력값 없음"); // ★★★ 3
        alert("주차장 주소를 입력하세요.");
        return;
    }

    // Tmap 라이브러리 로드 확인 (TData만 확인하면 됩니다)
    if (!window.Tmapv2 || !window.Tmapv2.extension || !window.Tmapv2.extension.TData) {
        alert("Tmap API 검색 라이브러리(TData)가 로드되지 않았습니다. 페이지를 새로고침 해주세요.");
        return;
    }
    
    const token = localStorage.getItem("token");

    // ★★★ 1. 주소 파싱 실행 (city_do, gu_gun 등으로 분리) ★★★
    const parsedAddress = parseAddress(newParkingAddress);
    if (!parsedAddress) {
        alert("주소 형식이 올바르지 않습니다. (예: 서울시 강남구 역삼동 123-4)");
        return;
    }
    // ★★★ 2단계(POI검색)를 위한 TData 객체 생성 (기존과 동일)
    const tData = new window.Tmapv2.extension.TData();

    // ★★★ 2. Tmap API 호출 (주소 -> 좌표) [TData.getGeoFromAddressJson 사용] ★★★
    const params = {
          // 데이터 로드 완료 시 (성공 콜백)
          onComplete: (resGeo) => { 
              const geoData = resGeo._responseData;
              
              // ★★★ TData 응답 구조에 맞게 유효성 검사 및 좌표 추출 ★★★
              // TData의 Geocoding 응답은 coordinateInfo 객체에 좌표를 담고 있습니다.
              if (!geoData.coordinateInfo || !geoData.coordinateInfo.lon || !geoData.coordinateInfo.lat) {
                  alert("주소 좌표 변환에 실패했습니다. 주소를 정확하게 입력해 주세요. (1단계)");
                  console.log("Tmap TData Geocoding 응답:", geoData);
                  return;
              }
              
              // 좌표 추출
              const latitude = Number(geoData.coordinateInfo.lat);
              const longitude = Number(geoData.coordinateInfo.lon);

              // --- 3. Tmap API 호출 (좌표 -> "주차장" POI 검색) ---
              const options = {
                  count: 1,
                  resCoordType: "WGS84GEO",
                  centerLon: longitude,
                  centerLat: latitude,
                  radius: 0.1, // 100m 반경
                  sort: 'distance'
              };
              
              tData.getPOIDataFromSearchJson("주차장", options, async (resPoi) => {
                  // ... (기존 2단계 POI 검색 성공 로직 그대로) ...
                  if (!resPoi || !resPoi._responseData) {
                      alert("Tmap POI 검색 실패: 응답 데이터가 유효하지 않습니다.");
                      return;
                  }

                  const poiData = resPoi._responseData;
                  if (!poiData.searchPoiInfo || !poiData.searchPoiInfo.pois || !poiData.searchPoiInfo.pois.poi[0]) {
                      alert("해당 주소 근처(100m)에서 주차장을 찾지 못했습니다. (2단계)");
                      return;
                  }
                  const place = poiData.searchPoiInfo.pois.poi[0];
                  
                  // --- 3. 백엔드 API 호출 (Tmap DTO -> parkingId 발급) ---
                  const tmapParkingList = [{
                      tmapPkey: place.pkey,
                      // ... (데이터 매핑 로직) ...
                      latitude: Number(place.noorLat) || Number(place.lat),
                      longitude: Number(place.noorLon) || Number(place.lon),
                      distance: Number(place.radius) || 0
                  }];
                  
                  try {
                      // ... (백엔드 추천 API 및 최종 등록 로직) ...
                      const recommendResponse = await axios.post(`${API_BASE_URL}/api/parking/recommend`, {
                          tmapParkingList: tmapParkingList,
                          filter: { isFree: false, isPublic: false }
                      });
                      
                      const parkingId = recommendResponse.data[0]?.parkingId;
                      if (!parkingId) {
                          throw new Error("주차장 ID를 백엔드에서 발급받지 못했습니다. (3단계)");
                      }
                      
                      const registerRequestDto = {
                          parkingId: parkingId,
                          additionalText: newParkingInfo
                      };
                      await axios.post(`${API_BASE_URL}/api/business/location/${currentLocationId}/parking`, registerRequestDto, {
                          headers: { Authorization: `Bearer ${token}` }
                      });
                      
                      alert("주차장이 성공적으로 등록되었습니다.");
                      setNewParkingAddress(""); 
                      setNewParkingInfo("");
                      fetchParkingsForLocation(currentLocationId);
                      setIsBusinessModalOpen(false);

                  } catch (error) {
                      console.error("주차장 등록 실패 (백엔드 3/4단계):", error);
                      alert("주차장 등록에 실패했습니다.");
                  }
              }, (error) => {
                  console.error("Tmap POI API 에러 (2단계):", error);
                  alert("주차장 검색 중 Tmap API 오류가 발생했습니다.");
              });
          }, 
          // 데이터 로드 중 에러가 발생시 (TData Geocoding API 자체 에러)
          onError: (error) => {
              console.error("Tmap TData Geocoding API 에러 (1단계):", error);
              alert("주소 검색 중 Tmap API 오류가 발생했습니다. 주소를 확인하세요.");
          }
      };
      // ★★★ TData API 호출 (getGeoFromAddressJson 사용) ★★★
      tData.getGeoFromAddressJson(
          parsedAddress.city_do,
          parsedAddress.gu_gun,
          parsedAddress.dong,
          parsedAddress.bunji,
          { coordType: "WGS84GEO" }, // optionObj: 좌표계 타입 설정
          params // params: onComplete, onError 콜백 함수
      );
  };

  // --- 확인 필요 메서드들 종료. ---

  // --- 함수 정의 종료. ---



  // --- JSX 렌더링. ---
  // JSX : JavaScript XML
  // App 컴포넌트의 최종 출력.
  // 이 return 결과가 index.html 의 <div id = "root">를 대체하게 됨.
  return (
    <div className="app-container">

      {/* 
      <MapView ... /> 태그 설명.
      1. 해당 태그가 인식되는 순간 MapView 함수를 실행시킴. 이 함수의 return인 <div> 태그가 현재 태그를 대체함.
      2. App.jsx의 handleMapReady 메서드를 MapView.jsx의 MapView()메서드에 onMapReady라는 이름으로 전달함.
      3. 메서드 참조 전달로 함수를 전달만 하고 이후 필요할 때 실행되게 함. handleMapReady : 메서드 참조 전달, handleMapReady() : 메서드 호출(실행).
      4. 진행 순서.
        4-1. <MapView... /> 태그 인식.
        4-2. MapView.jsx 파일의 MapView() 함수 실행. 이때 App.jsx의 handleMapReady 메서드를 onMapReady라는 이름으로 넘겨줌.
        4-3. return문으로 지도 뼈대 생성.
        4-4. useEffect 실행
          4-4-1. useEffect내의 initMap() 실행으로 지도 객체 생성. 해당 메서드 내의 조건문 if(onMapReady)에서 onMapReady(map, Tmapv2);가 실행됨.
          4-4-2. onMapReady(map, Tmapv2);의 실행으로 App.jsx 내의 handleMapReady();가 호출(실행)됨.
          4-4-3. initMap() 실행 후 useEffect의 return 대기.
          4-4-4. useEffect의 return 문은 어플리케이션의 종료 or 화면에서 사라질 때 return이 실행됨.      
      */}
      <MapView onMapReady={handleMapReady}/>


      {/* 로그인/로그아웃 버튼. */}
      {/* 조건에 따라서 해당 {...} 부분을 true, false에 따른 태그로 대체함. */}
      {isLoggedIn ? (
        // 로그인 상태일 때.
        <div className="user-menu-container">

          {/* 사업자 회원인 경우에만 뒤따라오는 <button>태그 렌더링. */}
          {userRole === 'ROLE_BUSINESS' && (
            <button className="login-button business-button" onClick={openBusinessModal}>
              마이페이지
            </button>
          )}

          {/* 로그아웃 버튼. */}
          <button className="login-button logout" onClick={handleLogout}>
            로그아웃
          </button>

        </div>
      ) : (
        // 로그아웃 상태일 때.
        <div className="user-menu-container">
          {/* 로그인 버튼. */}
          <button className="login-button" onClick={openModal}>
            로그인
          </button>
        </div>
      )}

      {/* TMAP 검색 UI. */}
      <div className="search-container">
        {/* 목적지 검색 후 확정되었을 때만 표시. */}
        {/* 확정된 목적지 정보, 필터 옵션 선택, 주변 주차장 검색 버튼 표시. */}
        {/* 논리 연산자 사용. selectedDestination이 true인 경우 &&이후를 적용하고, false인 경우 넘어감.  */}
        {selectedDestination && (
          <div className="destination-info">
            <strong>목적지: {selectedDestination.name}</strong>
            <span>{selectedDestination.address}</span>
            {/* 필터 UI */}
            <div className="filter-options">
              <label>
                <input 
                  type="checkbox" 
                  // 체크박스 체크 여부를 state의 true/false로 결정.
                  checked={filterOptions.isFree}
                  // 체크박스 클릭 시 이전 상태의 필터값을 가져온 후 무료 옵션만 이전과 반대로 변경.
                  onChange={() => setFilterOptions(prev => ({ ...prev, isFree: !prev.isFree }))}
                />
                무료만
              </label>
              <label>
                <input 
                  type="checkbox" 
                  // 체크박스 체크 여부를 state의 true/false로 결정.
                  checked={filterOptions.isPublic}
                  // 체크박스 클릭 시 이전 상태의 필터값을 가져온 후 공영 옵션만 이전과 반대로 변경.  
                  onChange={() => setFilterOptions(prev => ({ ...prev, isPublic: !prev.isPublic }))}
                />
                공영만
              </label>
            </div>
            <button className="parking-search-button" onClick={handleParkingSearch}>주변 주차장 검색하기</button>

            {/* 사업자 회원일 때만 이 버튼이 보임 */}
            {userRole === 'ROLE_BUSINESS' && (
              <button className="submit-button signup" style={{marginTop: '10px', width: '100%'}} onClick={handleRegisterLocation}> 이 장소를 내 사업장으로 등록 </button>
            )}

            {/* 목적지 초기화 버튼 */}
            <button className="reset-button" 
                    onClick={() => {
                      setSelectedDestination(null),
                      setParkingLots([]);
                      removeMarkers();
                      }}>
              목적지 초기화
            </button>
          </div>
        )}

        {/* 목적지 검색창. 목적지가 없을 때만 보임. */}
        {!selectedDestination && (
          <div className="search-box">
            <input 
              type="text" 
              placeholder="목적지를 입력하세요"
              // 입력창에 표시되는 텍스트를 keyword state와 일치시킴. 
              value={keyword}
              // 키보드를 칠때마다 발생하는 이벤트를 감지하여 상태 업데이트.
              // e : 이벤트 객체. target : 어떤것이 이 이벤트를 발생시켰는가? -> 현재는 input태그 요소를 가리킴. value : 현재 입력된 값.
              onChange={(e) => setKeyword(e.target.value)}
              // 누른 키가 엔터키인 경우 handleSearch()메서드 실행.
              // e : 이벤트 객체. 
              onKeyDown={(e) => e.key === 'Enter' && handleSearch()}
            />
            {/* 버튼 클릭 시 handleSearch()메서드 실행. */}
            <button onClick={handleSearch}>검색</button>
          </div>
        )}

        {/* Tmap 목적지 검색 결과 목록. 목적지가 없을 때만 보임 */}
        {!selectedDestination && (
          <ul className="search-results">
            {places.length === 0 ? (
              // 검색 결과가 없는 경우 표시.
              <li className="no-results">검색어를 입력하세요.</li>
            ) : (
              // 검색 결과가 있는 경우 표시.
              // Tmap API의 검색 결과 배열을 순회하면서 검색된 장소들의 목록을 뿌려주는 반복 렌더링.
              // place : 검색 결과 장소들이 담겨있는 배열의 개별 요소, index : 배열의 인덱스.
              // 응답 데이터 구조에 따라 place.id, place.name, place.address 필드명 확인 필요.
              places.map((place, index) => ( 
                // key로 사용할 것이 없으면 배열의 인덱스라도 임시로 사용.
                // 클릭 시 handlePlaceClick()메서드 실행.
                // 실행 시 해당 장소를 최종 목적지로 확정, 지도 화면 조정.
                <li key={place.id || place.pkey || index} onClick={() => handlePlaceClick(place)}> 
                  <strong>{place.name}</strong>
                  <span>{place.newAddressList?.newAddress?.[0]?.fullAddressRoad || place.address || '주소 정보 없음'}</span>
                </li>
              ))
            )}
          </ul>
        )}

        {/* 주차장 검색 로딩 표시 */}
        {isParkingLoading && (
          <div className = "search-loading">
            <p>주변 주차장 상세 정보를<br/>백엔드에서 가져오는 중입니다...</p>
          </div>
        )}

        {/* 로딩중이지 않고, 목적지가 선택되었을 경우에만 표시. */}
        {!isParkingLoading && selectedDestination && (
          <ul className="search-results">
            {parkingLots.length > 0 ? (
              // 백엔드 검색 결과 존재시 실행.
              // Tmap API의 검색 결과 배열을 순회하면서 검색된 주차장의 목록을 뿌려주는 반복 렌더링.
              parkingLots.map((place) => (
                // key : 리액트가 이 목록의 변경사항을 빠르고 효율적으로 감지하기 위해 사용하는 내부 식별표.
                // 클릭 시 백엔드에서 상세 정보 조회, 상세 정보 모달 창 생성.
                <li key={place.parkingId} onClick={() => handleParkingLotClick(place)}>
                  <strong>
                    {/* 사업자 추천 표시. */}
                    {place.isRecommended && (<span style={{ color: 'orange', marginRight: '5px' }}>★추천</span>)}
                    {/* 주차장 이름 표시. */}
                    {place.name}
                  </strong>
                  {/* 주차장 주소 표시. */}
                  <span>{place.address}</span>

                  {/* 주차장 상세 정보 표시. */}
                  <div className="parking-details">
                    <span style={{ color: '#007bff', fontWeight: 'bold' }}>
                      {/* 거리 표시 */}
                      목적지로부터 {
                        // toFixed : 지정한 소숫점까지 반올림하여 문자열로 반환.
                       (place.distance >= 1) 
                         ? `${place.distance.toFixed(3)} km`            // 1km 이상이면 km 표시.
                         : `${(place.distance * 1000).toFixed(0)} m`    // 1km 미만이면 m 표시.
                      }
                    </span>
                    <span style={{ color: '#6c757d' }}>
                      {/* 주차장 선택 횟수 표시. */}
                      (선택 {place.selectionCount} 회)
                    </span>
                  </div>

                  {/* 추가 정보 존재시 표시 */}
                  {place.additionalText && (
                    <div className="additional-info">
                      {place.additionalText}
                    </div>
                  )}
                </li>
              ))
            ) : (
              // 백엔드 검색 결과 가 없는 경우 실행.
              <li className="no-results">필터 조건에 맞는 주차장이 없습니다.</li>
            )}
          </ul>
        )}
      </div>
      
      {/* 주차장 상세 정보 모달 */}
      {selectedParkingDetail && (
        <div className="parking-detail-modal">
          {/* 상세 정보 모달 닫기 버튼. 클릭 시 state 변경. */}
          {/* 상태 변경 감지 시 App 컴포넌트를 처음부터 다시 호출. */}
          {/* 처음부터 다시 호출해도 state의 값들이 기본값으로 설정되지 않음. 메모리에 저장되어있는 값들이 있기 때문임. 브라우저 새로고침의 경우에는 기본값으로 변경됨. */}
          <button className="detail-close-button" onClick={() => setSelectedParkingDetail(null)}>
            {/* HTML에서 사용하는 특수 문자 코드. 닫기 버튼의 아이콘 표현 위해 X 기호로 렌더링 됨. */}
            &times;
          </button>

          {/* --- 주차장 명, 추천 주차장 여부. --- */}
          <div className="detail-header">
            <strong>
              {selectedParkingDetail.isRecommended && (
                <span style={{ color: 'orange', marginRight: '5px' }}>추천</span>
              )}
              {selectedParkingDetail.name}
            </strong>
          </div>
        
          {/* --- 기본 정보. --- */}
          {/* 주소, 거리, 선택 횟수, 사업자 제공 정보. */}
          <div className="detail-body">

            {/* 주소. */}
            <span>{selectedParkingDetail.address}</span>

            <div className="parking-details">
              {/* 거리. */}
              <span style={{ color: '#007bff', fontWeight: 'bold' }}>
                목적지로부터 {
                  (selectedParkingDetail.distance >= 1) 
                    ? `${selectedParkingDetail.distance.toFixed(1)} km`
                    : `${(selectedParkingDetail.distance * 1000).toFixed(0)} m`
                }
              </span>
              {/* 선택 횟수. */}
              <span style={{ color: '#6c757d' }}>
                (선택 {selectedParkingDetail.selectionCount} 회)
              </span>
            </div>

            {/* --- 사업자 제공 추가 정보. --- */}
            {selectedParkingDetail.additionalText && (
              <div className="additional-info">
                {selectedParkingDetail.additionalText}
              </div>
            )}
          </div>
          {/* --- 기본 정보 끝. --- */}
        
          {/* --- 추가 상세 정보. --- */}
          {/* 요금, 시간, 실시간 현황. */}
          <div className="detail-body" style={{paddingTop: 0}}>
            {isDetailLoading ? (
              // 로딩 중 표시.
              <div style={{ padding: '20px', textAlign: 'center', color: '#888' }}>
                <p>추가 상세 정보 로딩 중...</p>
              </div>
              ) : (
                // 로딩 종료 시 표시.
                <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', marginTop: '10px', borderTop: '1px dashed #ddd', paddingTop: '10px' }}>
                  {/* 요금 정보. */}
                  {selectedParkingDetail.priceInfo ? (
                    // 데이터 존재 시 표시.
                    <div className="parking-details" style={{borderTop: 'none', marginTop: 0, paddingTop: 0}}>
                      <span style={{fontWeight: 'bold'}}>요금:</span>
                      <span style={{textAlign: 'right'}}>{selectedParkingDetail.priceInfo}</span>
                    </div>
                    ) : (
                    // 데이터 미 존재 시 표시.
                    <div className="parking-details" style={{borderTop: 'none', marginTop: 0, paddingTop: 0}}>
                      <span style={{fontWeight: 'bold'}}>요금:</span>
                      <span style={{textAlign: 'right', color: '#888'}}>정보 없음</span>
                    </div>
                    )
                  }
              
                  {/* 운영 시간. */}
                  {selectedParkingDetail.operatingHours ? (
                    // 데이터 존재 시 표시.
                    <div className="parking-details" style={{borderTop: 'none', marginTop: 0, paddingTop: 0}}>
                      <span style={{fontWeight: 'bold'}}>운영:</span>
                      <span style={{textAlign: 'right'}}>{selectedParkingDetail.operatingHours}</span>
                    </div>
                    ) : (
                      // 데이터 미 존재 시 표시.
                      <div className="parking-details" style={{borderTop: 'none', marginTop: 0, paddingTop: 0}}>
                        <span style={{fontWeight: 'bold'}}>운영:</span>
                        <span style={{textAlign: 'right', color: '#888'}}>정보 없음</span>
                      </div>
                    )
                  }

                  {/* 실시간 주차 대수. */}
                  {/* 0이라도 표시하기 위해 null 체크. */}
                  {/* 주차 가능시 초록, 만차인 경우 빨간색. */}
                  {selectedParkingDetail.availableSpots != null ? (
                    // 데이터 존재 시 표시.
                    <div className="parking-details" style={{borderTop: 'none', marginTop: 0, paddingTop: 0, color: selectedParkingDetail.availableSpots > 0 ? '#28a745' : '#dc3545'}}>
                      <span style={{fontWeight: 'bold'}}>실시간:</span>
                      <span style={{fontWeight: 'bold'}}>
                        {selectedParkingDetail.availableSpots > 0 
                        ? `주차 가능: ${selectedParkingDetail.availableSpots} 대`
                        : '현재 만차'}
                      </span>
                    </div>
                    ) : (
                    // 데이터 미 존재 시 표시.
                    <div className="parking-details" style={{borderTop: 'none', marginTop: 0, paddingTop: 0}}>
                      <span style={{fontWeight: 'bold'}}>실시간:</span>
                      <span style={{textAlign: 'right', color: '#888'}}>정보 없음</span>
                    </div>
                    )
                  }
              
                {/* 향후 EV 충전소 등 다른 API 정보가 추가될 영역. */}
                </div>
              )
            }
          </div>
          {/* --- 추가 상세 정보 끝. --- */}

          {/* --- 푸터(결정 버튼). --- */}
          <div className="detail-footer">
            {/* 사업장에 추천 주차장으로 등록 버튼 표시. */}
            {currentLocationId ? (
              <button className="log-parking-button" onClick={() => handleFinalRegisterParking(selectedParkingDetail, currentLocationId)}>
                이 주차장을 사업장에 추천 등록
              </button>
              ) : (
                // currentLocationId가 없으면 (일반 사용자 모드) '결정' 버튼 표시 (기존 로직)
                <button className="log-parking-button" onClick={handleLogSubmit}>
                  이 주차장으로 결정
                </button>
              )
            }
          </div>
        </div>
      )}

      {/* --- 마이페이지 상세/수정 모달. --- */}
      {myPageModalDetail && ( 
        // 기존 주차장 상세 정보 모달 디자인 사용, 위치 수정.
        // 기존 left 스타일 초기화, 마이페이지(450px) 좌측에 20+20px 여백주고 위치. 마이페이지와 높이 맞춤, 기존 상세 모달과 동일 너비.
        <div className="parking-detail-modal" style={{left: 'auto', right: '490px', top: '70px', width: '300px'}}>
          {/* 닫기 버튼. 클릭 시 state 변경. */}
          <button className="detail-close-button" onClick={handleCloseMyParkingModal}>
            {/* HTML에서 사용하는 특수 문자 코드. 닫기 버튼의 아이콘 표현 위해 X 기호로 렌더링 됨. */}
            &times;
          </button>

          {/* 주차장 명. */}
          <div className="detail-header">
            <strong>{myPageModalDetail.parkingName}</strong>
          </div>
          
          {/* 주소, 수정 폼. */}
          <div className="detail-body">
            <span>
              {myPageModalDetail.parkingAddress}
            </span>
            
            {/* 추가 정보 수정 폼. */}
            <div className="form-group" style={{marginTop: '15px'}}>
              <label className="form-label">추가 정보 (수정)</label>
              {/* 스타일 재활용. */}
              <input type="text" className="auth-form" value={myPageModalText} onChange={(e) => setMyPageModalText(e.target.value)} placeholder="예: '입구는 후문입니다.'"/>
            </div>

          </div>

          {/* 수정 완료, 삭제 버튼. */}
          <div className="detail-footer">
            <div style={{display: 'flex', gap: '10px'}}>
              <button className="log-parking-button" onClick={handleUpdateMyParkingInfo} style={{width: '60%'}}>
                수정 완료
              </button>
              <button className="reset-button" onClick={handleDeleteMyParking} style={{width: '40%', margin: 0}}>
                삭제
              </button>
            </div>
          </div>

        </div>
      )}
      {/* --- 마이페이지 상세/수정 모달 끝. --- */}


      {/* 로그인, 회원가입 모달. */}
      {isModalOpen && (
        <div className="login-modal">
          <div className="modal-header">
            <h3>{authMode === 'login' ? '로그인' : '회원가입'}</h3>
            {/* 닫기 버튼. 클릭 시 state 변경. */}
            <button className="close-button" onClick={closeModal}>
              {/* HTML에서 사용하는 특수 문자 코드. 닫기 버튼의 아이콘 표현 위해 X 기호로 렌더링 됨. */}
              &times;
            </button>
          </div>
          
          <div className="modal-body">
            {/* 로그인 폼. */}
            {authMode === 'login' && (
              <form className="auth-form" onSubmit={handleLoginSubmit}>
                {/* required : 반드시 값을 입력해야만 폼을 제출할 수 있음. HTML의 표준 속성. */}
                <input type="text" placeholder="아이디" value={username} onChange={(e) => setUsername(e.target.value)} required/>
                <input type="password" placeholder="비밀번호" value={password} onChange={(e) => setPassword(e.target.value)} required/>

                {/* form 태그 안에 있는 버튼은 별도의 타입을 지정하지 않거나 submit타입으로 지정 시 폼을 제출하려는 성질을 가짐. */}
                <button className="submit-button login">로그인</button>

                {/* 회원가입 모드로 전환하는 텍스트. */}
                <div className="auth-switch">
                  {/* 클릭 시 회원가입 모드로 전환. */}
                  <span className="auth-switch-link" onClick={() => switchAuthMode('signup')}>
                    회원가입
                  </span>
                </div>
              </form>
            )}

            {/* 회원가입 폼. */}
            {authMode === 'signup' && (
              <form className="auth-form" onSubmit={handleSignupSubmit}>
                {/* required : 반드시 값을 입력해야만 폼을 제출할 수 있음. HTML의 표준 속성. */}
                <input type="text" placeholder="아이디" value={username} onChange={(e) => setUsername(e.target.value)} required/>
                <input type="password" placeholder="비밀번호" value={password} onChange={(e) => setPassword(e.target.value)} required/>
                <input type="email" placeholder="이메일" value={email} onChange={(e) => setEmail(e.target.value)} required/>

                <div className="form-group">
                  <label className="form-label">회원 구분</label>
                  <div className="radio-group">
                    <label>
                      <input type="radio" value="일반회원" checked={userType === '일반회원'} onChange={(e) => setUserType(e.target.value)}/>
                      일반회원
                    </label>
                    <label>
                      <input type="radio" value="사업자 회원" checked={userType === '사업자 회원'} onChange={(e) => setUserType(e.target.value)}/>
                      사업자 회원
                    </label>
                  </div>
                </div>

                {/* form 태그 안에 있는 버튼은 별도의 타입을 지정하지 않거나 submit타입으로 지정 시 폼을 제출하려는 성질을 가짐. */}
                <button className="submit-button signup">회원가입</button>

                {/* 회원가입 모드로 전환하는 텍스트. */}
                <div className="auth-switch">
                  <span className="auth-switch-link" onClick={() => switchAuthMode('login')}>
                    로그인
                  </span>
                </div>
              </form>
            )}

          </div>

        </div>
      )}

      {/* --- 사업자 마이페이지 모달. --- */}
      {isBusinessModalOpen && (
        // 스타일 재사용, 위치 속성 수정.
        // 오른쪽 경계에서 20px 띄움, 로그아웃 버튼 아래에 배치.
        <div className="login-modal" style={{ left: 'auto', right: '20px', top: '70px', width: '450px'}}>
          <div className="modal-header">
            <h3>사업자 마이페이지</h3>
            {/* 닫기 버튼. 클릭 시 state 변경. */}
            <button className="close-button" onClick={() => setIsBusinessModalOpen(false)}>
              {/* HTML에서 사용하는 특수 문자 코드. 닫기 버튼의 아이콘 표현 위해 X 기호로 렌더링 됨. */}
              &times;
            </button>
          </div>
          
          <div className="modal-body">
            {/* --- 내 사업장 목록. --- */}
            <h4>1. 내 사업장 목록</h4>
            <ul className="search-results business-location-list">
              {myLocations.length === 0 ? (
                // 등록된 사업장이 없는 경우.
                <li className="no-results">등록된 사업장이 없습니다.</li>
              ) : (
                myLocations.map((location) => (
                  // 현재 선택된 사업장이면 배경색 변경.
                  // 클릭 시 해당 사업장의 주차장 목록 로드.
                  // key : 리액트가 어떤 항목이 변경되었는지 식별하는 데 사용하는 고유한 식별자.
                  <li key={location.id} className={currentLocationId === location.id ? 'selected' : ''} onClick={() => fetchParkingsForLocation(location.id)}> 
                    <div style={{display: 'flex', justifyContent: 'space-between', alignItems: 'center'}}>
                      {/* 사업장 정보. */}
                      <div style={{display: 'flex', flexDirection: 'column'}}>
                        <strong>{location.locationName}</strong>
                        <span>{location.address}</span>
                      </div>
                      {/* 삭제 버튼. */}
                      {/* 이벤트 버블링을 막기 위해 e.stopPropagation() 호출. 클릭 이벤트가 더 이상 부모에게로 퍼지지 않고 그 자리에서 멈춤.*/}
                      <button type="button" className="reset-button" style={{margin: 0, padding: '5px 8px', fontSize: '12px', flexShrink: 0}} onClick={(e) => {e.stopPropagation(); handleDeleteLocation(location.id, location.locationName);}}>
                        삭제
                      </button>
                    </div>
                  </li>
                ))
              )}
            </ul>

            {/* --- 선택된 사업장의 주차장 관리. --- */}
            <h4 style={{marginBottom: '10px'}}>
              2. 추천 주차장 수동 등록
            </h4>
            
            {/* --- 주소 검색 UI. --- */}
            <div className="search-box" style={{padding: '10px 0'}}>
              {/* 사업장 미 선택시 비활성화. */}
              <input type="text" placeholder="주차장 주소 또는 키워드 입력" value={manualSearchKeyword} 
                onChange={(e) => setManualSearchKeyword(e.target.value)} 
                disabled={!currentLocationId}
                onKeyDown={(e) => e.key === 'Enter' && handleManualSearch()}/>
                {/* 새 핸들러 연결. */}
              <button onClick={handleManualSearch} disabled={!currentLocationId}>
                검색
              </button>
            </div>

            {/* --- 주소 검색 결과 목록. --- */}
            {manualSearchResults.length > 0 && (
              <ul className="search-results" style={{maxHeight: '150px', marginTop: '10px'}}>
                {manualSearchResults.map((place, index) => (
                  // 리스트 클릭 시 장소 선택.
                  <li key={place.id || place.pkey || index} onClick={() => handleManualPlaceSelect(place)}>
                    <strong>{place.name}</strong>
                    <span>{place.newAddressList?.newAddress?.[0]?.fullAddressRoad || place.address || '주소 정보 없음'}</span>
                  </li>
                ))}
              </ul>
            )}

            {/* --- 최종 등록 폼. --- */}
            {/* 장소가 선택되었을 때만 이름 입력 폼 표시 */}
            {manualSelectedPlace && (
              <form className="auth-form" onSubmit={handleFinalManualRegister} style={{backgroundColor: '#f8f9fa', padding: '15px', borderRadius: '8px', marginTop: '15px'}}>
                <div className="form-group">
                  <label className="form-label">선택된 주소</label>
                  {/* search-box 레이아웃을 재활용하여 input과 button을 나란히 배치 */}
                  <div className="search-box" style={{padding: '0'}}>
                    {/* 주소는 읽기 전용. readonly 속성. */}
                    <input type="text" value={manualSelectedPlace.newAddressList?.newAddress?.[0]?.fullAddressRoad || manualSelectedPlace.address || ''} readOnly style={{backgroundColor: '#e9ecef'}}/>
                    {/* 선택 취소 버튼을 input 바로 옆으로 이동 */}
                    {/* 선택된 장소, 입력했던 이름, 추가 정보 초기화. */}
                    <button type="button" style={{margin: 0}} 
                      onClick={() => {
                        setManualSelectedPlace(null); // 선택된 장소(POI)를 null로 초기화.
                        setManualParkingName("");     // 입력했던 이름 초기화.
                        setNewParkingInfo("");        // 입력했던 추가 정보 초기화.
                      }}>
                      취소
                    </button>
                  </div>
                </div>

                <div className="form-group">
                  {/* 이름은 필수 입력 옵션임. */}
                  <label className="form-label">등록할 주차장 이름</label>
                  <input type="text" placeholder="예: 시청 부설 주차장" value={manualParkingName} onChange={(e) => setManualParkingName(e.target.value)} required/>
                </div>

                {/* 추가 정보. */}
                <div className="form-group">
                  <label className="form-label">추가 정보 (선택)</label>
                  <input type="text" placeholder="예: '입구는 후문입니다.'" value={newParkingInfo} onChange={(e) => setNewParkingInfo(e.target.value)}/>
                </div>

                {/* disabled 속성 사용으로 이름이 입력되어야 버튼이 활성화. */}
                <button className="submit-button signup" type="submit" disabled={!manualParkingName}>
                  이 주차장 추천하기
                </button>
              </form>
            )}

            {/* 사업장이 선택되었을 때만 이름과 목록을 모두 표시. */}
            {currentLocationId && (
              // 빈 태그의 명칭 : 리액트 프래그먼트.
              // 리액트의 문법 -> 반드시 하나의 부모 태그가 감싸고 있어야 함.
              // 조건문 안의 두 요소를 하나의 묶음으로 포장하기 위함.
              // div태그를 쓰지 않은 이유 : HTML 구조를 망치지 않기 위함.
              <>
                {/* 동적 제목 추가. */}
                <h4 style={{marginTop: '20px', marginBottom: '10px'}}>
                  {/* 사업장 목록 state에서 현재 ID와 일치하는 사업장 이름 찾음. */}
                  [{myLocations.find(l => l.id === currentLocationId)?.locationName}]에 등록된 추천 주차장
                </h4>
                
                {/* 기존 <ul> 목록을 <h4> 태그 아래로 이동. */ }
                <ul className="search-results" style={{maxHeight: '150px', marginTop: '10px'}}>
                  {myLocationParkings.length === 0 ? (
                    // 데이터 미 존재 시.
                    <li className="no-results">이 사업장에 등록된 추천 주차장이 없습니다.</li>
                  ) : (
                    // 데이터 존재 시.
                    myLocationParkings.map((parking) => (
                      <li key={parking.id} style={{cursor: 'pointer'}} onClick={() => handleOpenMyParkingModal(parking)}>
                        <strong>{parking.parkingName}</strong>
                        <span>{parking.parkingAddress}</span>
                        {parking.additionalText && (
                          <span style={{color: '#28a745', marginTop: '5px'}}>
                            내 정보: {parking.additionalText}
                          </span>
                        )}
                      </li>
                    ))
                  )}
                </ul>
              </>
            )}

          </div>
        </div>
      )}
      {/* --- 사업자 마이페이지 모달 끝. --- */}

    </div>
  );
}

export default App;
