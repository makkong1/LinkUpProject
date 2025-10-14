document.addEventListener("DOMContentLoaded", function() {
    // 로그인 폼 입력 필드들에 대한 처리
    const loginInputs = document.querySelectorAll('#id, #password');
    
    loginInputs.forEach(input => {
        const formGroup = input.closest('.form-group');
        const label = formGroup.querySelector('label');
        
        // Clear 버튼 생성
        const clearButton = document.createElement("button");
        clearButton.type = "button";
        clearButton.classList.add("clear-button");
        clearButton.innerHTML = '<i class="fas fa-times"></i>';
        clearButton.style.cssText = `
            position: absolute;
            right: 1rem;
            top: 50%;
            transform: translateY(-50%);
            background: none;
            border: none;
            color: var(--gray-400);
            cursor: pointer;
            font-size: 0.9rem;
            opacity: 0;
            transition: opacity 0.3s ease;
            z-index: 10;
        `;
        
        // Clear 버튼을 form-group에 추가
        formGroup.style.position = 'relative';
        formGroup.appendChild(clearButton);
        
        // 입력값 변경 시 clear 버튼 표시/숨김
        input.addEventListener('input', function() {
            if (input.value.trim()) {
                clearButton.style.opacity = '1';
            } else {
                clearButton.style.opacity = '0';
            }
        });
        
        // Clear 버튼 클릭 시 입력값 초기화
        clearButton.addEventListener('click', function(event) {
            event.preventDefault();
            event.stopPropagation();
            input.value = '';
            input.focus();
            clearButton.style.opacity = '0';
            
            // 라벨을 원래 위치로 되돌리기
            if (label) {
                label.style.top = '50%';
                label.style.transform = 'translateY(-50%)';
                label.style.fontSize = '1rem';
                label.style.color = 'var(--gray-500)';
                label.style.background = 'none';
                label.style.padding = '0';
            }
        });
        
        // 포커스 시 clear 버튼 표시 (값이 있을 때만)
        input.addEventListener('focus', function() {
            if (input.value.trim()) {
                clearButton.style.opacity = '1';
            }
        });
        
        // 블러 시 clear 버튼 숨김
        input.addEventListener('blur', function() {
            setTimeout(() => {
                if (!input.value.trim()) {
                    clearButton.style.opacity = '0';
                }
            }, 150);
        });
    });
    
    // 로그인 폼 제출 시 처리
    const loginForm = document.querySelector('.login-form');
    if (loginForm) {
        loginForm.addEventListener('submit', function(e) {
            const idInput = document.getElementById('id');
            const passwordInput = document.getElementById('password');
            
            // 기본 유효성 검사
            if (!idInput.value.trim()) {
                e.preventDefault();
                showError('아이디를 입력해주세요.');
                idInput.focus();
                return;
            }
            
            if (!passwordInput.value.trim()) {
                e.preventDefault();
                showError('비밀번호를 입력해주세요.');
                passwordInput.focus();
                return;
            }
            
            // 로딩 상태 표시
            const submitBtn = loginForm.querySelector('.login-btn');
            if (submitBtn) {
                submitBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> 로그인 중...';
                submitBtn.disabled = true;
            }
        });
    }
    
    // 에러 메시지 표시 함수
    function showError(message) {
        // 기존 에러 메시지 제거
        const existingError = document.querySelector('.error-message');
        if (existingError) {
            existingError.remove();
        }
        
        // 새 에러 메시지 생성
        const errorDiv = document.createElement('div');
        errorDiv.className = 'error-message';
        errorDiv.textContent = message;
        
        // 로그인 카드에 에러 메시지 추가
        const loginCard = document.querySelector('.login-card');
        if (loginCard) {
            loginCard.appendChild(errorDiv);
            
            // 에러 메시지가 보이도록 스크롤
            errorDiv.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
        }
    }
    
    // 소셜 로그인 버튼 클릭 시 처리
    const socialButtons = document.querySelectorAll('.social-btn');
    socialButtons.forEach(button => {
        button.addEventListener('click', function(e) {
            // 로딩 상태 표시
            const originalText = this.innerHTML;
            this.innerHTML = '<i class="fas fa-spinner fa-spin"></i> 연결 중...';
            this.style.pointerEvents = 'none';
            
            // 3초 후 원래 상태로 복원 (실제로는 리다이렉트되지만 안전장치)
            setTimeout(() => {
                this.innerHTML = originalText;
                this.style.pointerEvents = 'auto';
            }, 3000);
        });
    });
    
    // Enter 키로 로그인 폼 제출
    document.addEventListener('keydown', function(e) {
        if (e.key === 'Enter') {
            const activeElement = document.activeElement;
            if (activeElement && (activeElement.id === 'id' || activeElement.id === 'password')) {
                const loginForm = document.querySelector('.login-form');
                if (loginForm) {
                    loginForm.dispatchEvent(new Event('submit'));
                }
            }
        }
    });
});
