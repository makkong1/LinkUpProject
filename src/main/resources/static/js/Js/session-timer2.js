// CSRF 토큰 가져오기
function getCsrfToken() {
    return document.querySelector('meta[name="_csrf"]').getAttribute('content');
}

// CSRF 헤더 이름 가져오기
function getCsrfHeader() {
    return document.querySelector('meta[name="_csrf_header"]').getAttribute('content');
}

// 세션 경고 알림 함수
function sessionWarning() {
    console.log("세션 경고 알림 호출됨"); // 디버깅 메시지 추가

    const toast = Toastify({
        text: "세션이 5분 후에 만료됩니다. 연장하시겠습니까?",
        duration: 5 * 60 * 1000, // 5분
        close: true, // 닫기 버튼 추가
        gravity: "bottom", // 화면 하단에 표시
        position: "right", // 오른쪽에 표시
        stopOnFocus: false, // 클릭하면 알림이 사라지도록 false로 설정
        onClick: function () {
            extendSession();  // 세션 연장 요청
            toast.hideToast();  // 클릭 시 알림 닫기
        },
        style: {
            zIndex: 9999, // 다른 요소들 위에 표시되도록 z-index를 설정
            background: "#4CAF50"  // backgroundColor를 style.background로 변경
        }
    });

    toast.showToast();  // 알림 표시
}

// "세션 연장" 요청 함수
function extendSession() {
    $.ajax({
        url: '/api/extend-session',
        method: 'POST',
        xhrFields: {withCredentials: true}, // 세션을 유지하기 위해 필요
        headers: {
            [getCsrfHeader()]: getCsrfToken() // CSRF 토큰을 요청 헤더에 포함
        },
        success: function () {
            Toastify({
                text: "세션이 연장되었습니다.",
                duration: 3000, // 3초간 표시
                gravity: "bottom",
                position: "right",
                backgroundColor: "#4CAF50",
            }).showToast();
        },
        error: function () {
            Toastify({
                text: "세션 연장에 실패했습니다. 다시 로그인해주세요.",
                duration: 5000, // 5초간 표시
                gravity: "bottom",
                position: "right",
                backgroundColor: "#FF0000",
            }).showToast();
            window.location.href = '/users/loginP';
        }
    });
}

// 세션 타이머 시작
function startSessionTimer() {
    console.log("세션 타이머 시작됨"); // 디버깅 메시지 추가
    var sessionTimeout = 30 * 60 * 1000; // 30분 (ms 단위)
    var warningTime = sessionTimeout - (5 * 60 * 1000); // 1분 전 경고 (5분 남았을 때 경고)

    // 세션 만료 전 경고 알림
    setTimeout(function () {
        sessionWarning(); // 5분 전에 경고 알림
    }, warningTime);

    // 세션 만료 후 로그아웃
    setTimeout(function () {
        window.location.href = '/logout'; // 세션 만료 시 로그아웃
    }, sessionTimeout);
}

// 타이머 시작
startSessionTimer();
