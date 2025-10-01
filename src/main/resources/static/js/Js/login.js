    <script>
        document.addEventListener("DOMContentLoaded", function() {
            const inputs = document.querySelectorAll('input');

            inputs.forEach(input => {
                const label = input.nextElementSibling;
                const clearButton = document.createElement("button");
                clearButton.classList.add("clear-button");
                clearButton.textContent = "지우기";
                input.parentNode.appendChild(clearButton);

                // 입력값이 있으면 '전체 지우기' 버튼 보이기
                input.addEventListener('input', function() {
                    if (input.value) {
                        clearButton.style.display = 'block';
                    } else {
                        clearButton.style.display = 'none';
                    }
                });

                // '전체 지우기' 클릭 시 입력값 초기화
                clearButton.addEventListener('click', function(event) {
                    event.preventDefault();  // 폼 제출 방지
                    input.value = '';
                    clearButton.style.display = 'none';
                    input.focus();
                });

                // 아이디 입력창(u_id)만 라벨 위치 변경
                if (input.id === 'id' || input.id === 'password') {
                    if (input.value) {
                        label.style.top = '10px';
                        label.style.fontSize = '12px';
                        label.style.color = '#007bff';
                        label.style.left = '13px'; // 아이디만 13px로 위치 변경
                        clearButton.style.display = 'block'; // 입력값이 있으면 버튼 보이기
                    }

                    // 포커스 시 아이디의 라벨 위치 변경
                    input.addEventListener('focus', function() {
                        label.style.top = '10px';
                        label.style.fontSize = '12px';
                        label.style.color = '#007bff';
                        label.style.left = '13px'; // 아이디의 라벨 위치 변경
                    });

                    // 블러 시 아이디의 라벨 원위치
                    input.addEventListener('blur', function() {
                        if (!input.value) {
                            label.style.top = '17px';
                            label.style.fontSize = '16px';
                            label.style.color = '#555';
                            label.style.left = '10px'; // 기본 left 값으로 되돌리기
                            clearButton.style.display = 'none';
                        }
                    });
                }
            });
        });
    </script>

    <script>
        document.addEventListener("DOMContentLoaded", function() {
            const errorMessageElement = document.getElementById("errorMessage");

            // 세션에서 errorMessage를 가져옴
            const errorMessageFromSession = sessionStorage.getItem("errorMessage");

            // 세션에 실패 메시지가 있을 때 처리
            if (errorMessageFromSession) {
                errorMessageElement.textContent = errorMessageFromSession;
                sessionStorage.removeItem("errorMessage");  // 표시 후 세션에서 제거
            } else {
                // 실패 메시지가 없을 경우 기본 메시지 "다시 로그인해주세요" 표시
                errorMessageElement.textContent = "다시 로그인해주세요";
            }
        });
    </script>
