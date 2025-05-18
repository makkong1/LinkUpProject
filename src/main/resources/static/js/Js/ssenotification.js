function startSSENotifications(userId) {
    // 익명 사용자 차단
    if (!userId || userId === "Anonymous") {
        console.log("로그인하지 않은 사용자는 SSE 연결을 하지 않습니다.");
        return;
    }

    // SSE 연결 시작
    const eventSource = new EventSource(`/users/notifications/${userId}`);

    eventSource.onmessage = function (event) {
        const message = event.data;
        showNotification(message);
    };

    eventSource.onerror = function (event) {
        console.error("SSE 연결 오류 발생:", event);
        eventSource.close(); // 오류 발생 시 연결 종료 (무한 재시도 방지)
    };
}

function showNotification(message) {
    const modal = $('<div class="modal"></div>').html(message);

    modal.css({
        position: 'fixed',
        bottom: '20px',
        right: '20px',
        background: 'rgba(0, 0, 0, 0.7)',
        color: 'white',
        padding: '10px 20px',
        borderRadius: '5px',
        fontSize: '16px',
        zIndex: 999,
        maxWidth: '300px',
        boxShadow: '0 4px 8px rgba(0, 0, 0, 0.2)',
        opacity: 0,  // 처음에는 투명하게 시작
        transition: 'opacity 0.5s'  // 점차 불투명하게 변하는 애니메이션
    });

    $('body').append(modal);

    // 모달이 화면에 추가된 후에 opacity를 1로 변경
    setTimeout(() => {
        modal.css('opacity', 1);  // 알림을 서서히 나타나게 함
    }, 100);  // 약간의 지연을 줘서 DOM에 완전히 추가된 후 실행

    setTimeout(() => {
        modal.fadeOut(500, () => modal.remove());  // 일정 시간 후 서서히 사라지게 함
    }, 3000);  // 3초 후에 사라지기 시작
}


