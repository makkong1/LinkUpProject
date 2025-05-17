document.addEventListener('DOMContentLoaded', function () {
    const userId = $('#userId').data('username');
    if (userId) {
        startSSENotifications(userId); // SSE 알림 시작
    }
});

// 검색 기능
function textSearch() {
    let select_value = document.getElementById("search").value;
    let text = document.getElementById("input_text").value;
    let select_comment_value = document.getElementById("search_comment").value;
    let text_comment = document.getElementById("input_text_comment").value;

    if (select_value !== 'all' && text === '' || select_comment_value !== "all_comment" && text_comment === '') {
        alert("검색어를 입력해 주세요");
        return;
    }
    let url = `/admin/listForAdminP?select_value=${encodeURIComponent(select_value)}&text=${encodeURIComponent(text)}&select_comment_value=${encodeURIComponent(select_comment_value)}&text_comment=${encodeURIComponent(text_comment)}`;
    location.href = url;
}

$(document).ready(function () {
    // 탭 클릭 시 해당 내용 보여주기
    $(".tab-btn").click(function () {
        let targetTab = $(this).data("target");
        $(".tab-content").hide(); // 모든 탭 숨기기
        $("#" + targetTab).show(); // 클릭한 탭만 보이게 하기
        $(".tab-btn").removeClass("active"); // 모든 버튼에서 active 제거
        $(this).addClass("active"); // 클릭한 버튼에 active 추가
    });

    $(".tab-btn").first().click(); // 기본적으로 첫 번째 탭 보이기

    // AJAX 요청 함수
    function sendRequest(url, method, data, successCallback, errorCallback) {
        $.ajax({
            url: url,
            method: method,
            data: data,
            beforeSend: function (xhr) {
                const csrfToken = $("meta[name='_csrf']").attr("content");
                const csrfHeader = $("meta[name='_csrf_header']").attr("content");
                xhr.setRequestHeader(csrfHeader, csrfToken);
            },
            success: successCallback,
            error: errorCallback
        });
    }

    // 신고 처리 및 삭제 버튼 클릭 이벤트 바인딩 (이벤트 위임)
    $(document).on("click", ".report-action-button", function () {
        const bIdx = $(this).data("id");
        const action = $(this).data("action");

        if (action === "delete") {
            let confirmDelete = confirm('이 게시글을 삭제하시겠습니까?');
            if (confirmDelete) {
                sendRequest(`/admin/board/${bIdx}/delete`, 'POST', {}, function (response) {
                    if (response === "success") {
                        alert("게시글이 삭제되었습니다.");
                        location.reload();
                    } else {
                        alert("게시글 삭제 실패");
                    }
                }, function () {
                    alert("요청 실패");
                });
            }
        } else if (action === "resolve") {
            sendRequest(`/admin/board/${bIdx}/resolve`, 'POST', {}, function (response) {
                if (response === "success") {
                    alert("게시글 신고가 해결되었습니다.");
                    location.reload();
                } else {
                    alert("신고 해결 실패");
                }
            }, function () {
                alert("요청 실패");
            });
        }
    });

    // 댓글 신고 처리 및 삭제 버튼 클릭 이벤트 바인딩 (이벤트 위임)
    $(document).on("click", ".comment-action-button", function () {
        const cIdx = $(this).data("id");
        const action = $(this).data("action");

        if (action === "delete") {
            let confirmDelete = confirm('이 댓글을 삭제하시겠습니까?');
            if (confirmDelete) {
                sendRequest(`/admin/comment/${cIdx}/delete`, 'POST', {}, function (response) {
                    if (response === "success") {
                        alert("댓글이 삭제되었습니다.");
                        location.reload(); // 삭제 후 새로고침
                    } else {
                        alert("댓글 삭제 실패");
                    }
                }, function () {
                    alert("요청 실패");
                });
            }
        } else if (action === "resolve") {
            sendRequest(`/admin/comment/${cIdx}/resolve`, 'POST', {}, function (response) {
                if (response === "success") {
                    alert("댓글 신고가 해결되었습니다.");
                    location.reload(); // 신고 해결 후 새로고침
                } else {
                    alert("신고 해결 실패");
                }
            }, function () {
                alert("요청 실패");
            });
        }
    });
});