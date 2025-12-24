// 공통 error 처리 함수
function handleError(response) {
    if (response.status === 401) {
        const redirectLoginP = confirm("로그인이 필요합니다. 로그인화면으로 가시겠습니까?");
        if (redirectLoginP) {
            window.location.href = "/users/loginP";
        }
    } else if (response.status === 403) {
        alert("접근 권한이 없습니다.");
    } else {
        alert("요청 처리에 실패했습니다.");
        console.error("Error: ", response);
    }
}

// CSRF 토큰 가져오기
function getCsrfHeaders() {
    const token = document.querySelector("meta[name='_csrf']").getAttribute("content");
    const header = document.querySelector("meta[name='_csrf_header']").getAttribute("content");
    return { [header]: token };
}

// DOMContentLoaded 처리
document.addEventListener("DOMContentLoaded", function() {
    const userId = document.getElementById("userId")?.dataset.username;
    if (userId) {
        startSSENotifications(userId); // SSE 알림 시작
    } else {
        console.log("로그인 필요");
    }

    // 댓글 등록
    const commentSubmit = document.getElementById("commentSubmit");
    if (commentSubmit) {
        commentSubmit.addEventListener("click", function() {
            const commentData = {
                c_content: document.getElementById("commentContent").value,
                c_writer: document.getElementById("writerId").value,
                b_idx: document.getElementById("boardId").value,
                uEmail: document.getElementById("writerEmail").value
            };

            if (!commentData.c_content.trim()) {
                alert("댓글을 입력해주세요.");
                return;
            }

            fetch("/users/board/comments", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    "X-Requested-With": "XMLHttpRequest",
                    ...getCsrfHeaders()
                },
                body: JSON.stringify(commentData)
            })
            .then(res => {
                if (!res.ok) throw res;
                return res.json();
            })
            .then(response => {
                const newCommentHtml = `
                    <div class="comment">
                        <strong>${response.c_idx} 번</strong>
                        <p><strong>${response.c_writer}</strong>: ${response.c_content}</p>
                        <p><small>작성일: ${response.c_upLoad}</small></p>
                        <div>
                            <input type="button" value="삭제" data-comment-id="${response.c_idx}" class="delete-comment-btn"/>
                        </div>
                    </div>
                `;
                document.getElementById("commentList").insertAdjacentHTML("beforeend", newCommentHtml);
                document.getElementById("commentContent").value = "";
            })
            .catch(handleError);
        });
    }

    // 좋아요 / 싫어요 공통 함수
    function handleLikeDislike(buttonId, countId, urlSuffix) {
        const button = document.getElementById(buttonId);
        if (!button) return;

        button.addEventListener("click", function() {
            const boardId = button.dataset.boardId;
            const countEl = document.getElementById(countId);
            const currentCount = parseInt(countEl.textContent || "0");
            countEl.textContent = currentCount + 1;

            fetch(`/board/${boardId}/${urlSuffix}`, {
                method: "POST",
                headers: { "Content-Type": "application/json", ...getCsrfHeaders() }
            })
            .then(res => {
                if (!res.ok) throw res;
                return res.json();
            })
            .then(response => {
                countEl.textContent = urlSuffix === "like" ? response.likeCount : response.dislikeCount;
            })
            .catch(handleError);
        });
    }

    handleLikeDislike("likeButton", "likeCount", "like");
    handleLikeDislike("dislikeButton", "dislikeCount", "dislike");

    // 댓글 좋아요/싫어요 처리
    document.getElementById("commentList")?.addEventListener("click", function(e) {
        const target = e.target;
        if (target.classList.contains("comment-like-button") || target.classList.contains("comment-dislike-button")) {
            const isLike = target.classList.contains("comment-like-button");
            const commentId = target.dataset.commentId;
            const countEl = target.parentElement.querySelector(isLike ? ".comment-like-count" : ".comment-dislike-count");
            countEl.textContent = (parseInt(countEl.textContent || "0") + 1);

            fetch(`/users/comment/${commentId}/${isLike ? "like" : "dislike"}`, {
                method: "POST",
                headers: { "Content-Type": "application/json", ...getCsrfHeaders() }
            })
            .then(res => {
                if (!res.ok) throw res;
                return res.json();
            })
            .then(response => {
                countEl.textContent = isLike ? response.likeCount : response.dislikeCount;
            })
            .catch(handleError);
        }

        // 댓글 삭제
        if (target.classList.contains("delete-comment-btn")) {
            const commentId = target.dataset.commentId;
            if (!confirm("삭제 후 취소할 수 없습니다. 삭제하시겠습니까?")) return;

            fetch(`/users/board/comments/${commentId}`, {
                method: "POST",
                headers: { "Content-Type": "application/json", ...getCsrfHeaders() },
                body: JSON.stringify({ commentId })
            })
            .then(res => {
                if (!res.ok) throw res;
                return res.json();
            })
            .then(() => location.reload())
            .catch(handleError);
        }

        // 댓글 신고
        if (target.id === "reportComment") {
            const commentId = target.dataset.commentId;
            if (!confirm("신고하시겠습니까?")) return;

            fetch(`/users/board/comment/${commentId}/report`, {
                method: "POST",
                headers: { "Content-Type": "application/json", ...getCsrfHeaders() },
                body: JSON.stringify({ forReportCommentId: commentId })
            })
            .then(res => {
                if (!res.ok) throw res;
                return res.json();
            })
            .then(() => location.reload())
            .catch(handleError);
        }
    });
});
