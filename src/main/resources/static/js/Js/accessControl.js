// 권한이 없는 사용자가 권한이 필요한 요청을 할 때 처리하는 함수
function handleAction(actionUrl) {
    fetch(actionUrl)
        .then(response => {
            if (!response.ok) {
                if (response.status === 401) {
                    // 로그인하지 않은 사용자일 경우 로그인 페이지로 리다이렉트
                    let redirectLoginP = confirm("로그인이 필요합니다. 로그인 화면으로 가시겠습니까?");
                    if (redirectLoginP) {
                        window.location.href = "/users/loginP"; // 로그인 페이지로 리다이렉트
                    }
                } else if (response.status === 403) {
                    // 권한이 없는 사용자가 요청한 경우
                    response.json().then(data => {
                        alert(data.message); // JSON 응답에서 메시지 받아 alert로 표시
                    });
                }
            } else {
                return response.json(); // 정상적으로 처리된 경우
            }
        })
        .catch(error => console.error("Error:", error));
}