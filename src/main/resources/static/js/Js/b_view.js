// 공통 error 처리 함수
function handleError(xhr, status, error) {
  if (xhr.status === 401) {
    let redirectLoginP = confirm("로그인이 필요합니다. 로그인화면으로 가시겠습니까?");
    if (redirectLoginP) {
      window.location.href = "/users/loginP"; // 로그인 페이지로 리디렉션
    }
  } else if (xhr.status === 403) {
    // 권한 부족 시 403 응답을 받으면 권한 없음 메시지 표시
    alert("접근 권한이 없습니다.");
  } else {
    console.log("Error: " + error);
    alert("요청 처리에 실패했습니다.");
  }
}

document.addEventListener("DOMContentLoaded", function () {
  const userId = $("#userId").data("username"); // HTML에서 userId를 가져온다고 가정
  const userEmail = $("#userEmail").data("email");
  if (userId) {
    startSSENotifications(userId); // SSE 알림 시작
  } else {
    console.log("로그인 필요");
  }
});

// CSRF 토큰을 AJAX 요청에 추가
$(document).ajaxSend(function (event, xhr) {
  const csrfToken = $("meta[name='_csrf']").attr("content");
  const csrfHeader = $("meta[name='_csrf_header']").attr("content");
  xhr.setRequestHeader(csrfHeader, csrfToken);
});

$(document).ready(function () {
  // 댓글 등록
  $(document).on("click", "#commentSubmit", function () {
    let commentData = {
      c_content: $("#commentContent").val(), // 댓글 내용
      c_writer: $("#writerId").val(), // 댓글 작성자 ID
      b_idx: $("#boardId").val(), // 게시글 ID
      uEmail: $("#writerEmail").val(),
    };
// commentData 출력
    console.log(commentData);
    // 댓글이 빈 경우 처리
    if (commentData.c_content.trim() === "") {
      alert("댓글을 입력해주세요.");
      return;
    }

    // AJAX 요청으로 댓글 등록
    $.ajax({
      url: "/users/board/comments",
      method: "POST",
      data: JSON.stringify(commentData),
      contentType: "application/json",
      success: function (response) {
        const newCommentHtml = `
          <div class="comment">
            <strong>${response.c_idx} 번</strong>
            <p>
              <strong>${response.c_writer}</strong>: ${response.c_content}
            </p>
            <p><small>작성일: ${response.c_upLoad}</small></p>
            <div>
              <input type="button" value="삭제" data-comment-id="${response.c_idx}" />
            </div>
          </div>
        `;
        $("#commentList").append(newCommentHtml);  // 기존 댓글 하단에 추가
        $("#commentContent").val("");              // 입력창 초기화
      },
      error: handleError, // error 처리 함수 호출
    });
  });

// 좋아요 증가
$(document).on("click", "#likeButton", function () {
  const boardId = $(this).data("board-id");

  $.ajax({
    url: "/board/" + boardId + "/like",
    method: "POST",
    contentType: "application/json",
    success: function (response) {
    console.log($("#likeCount").length);  // 0이면 선택자 문제
      $("#likeCount").text(response.likeCount); // 값만 갱신
    },
    error: handleError,
  });
});

// 싫어요 증가
$(document).on("click", "#dislikeButton", function () {
  const boardId = $(this).data("board-id");

  $.ajax({
    url: "/board/" + boardId + "/dislike",
    method: "POST",
    contentType: "application/json",
    success: function (response) {
      $("#dislikeCount").text(response.dislikeCount); // 값만 갱신
    },
    error: handleError,
  });
});

  // 댓글 삭제
  $(document).on("click", "#deleteComment", function () {
    const commentId = $(this).data("comment-id"); // 삭제할 댓글의 ID 가져오기

    const isConfirmed = confirm("삭제 후 취소할 수 없습니다. 삭제하시겠습니까?");
    if (!isConfirmed) {
      return;
    }

    // AJAX 요청으로 댓글 삭제 처리
    $.ajax({
      url: "/users/board/comments/" + commentId,
      method: "POST",
      data: JSON.stringify({ commentId: commentId }), // 삭제할 댓글의 ID를 전달
      contentType: "application/json",
      success: function (response) {
        alert("댓글을 삭제했습니다.");
        window.location.reload();
      },
      error: handleError, // error 처리 함수 호출
    });
  });

  // 댓글 신고
  $(document).on("click", "#reportComment", function () {
    const forReportCommentId = $(this).data("comment-id"); // 신고할 댓글의 id

    const isFired = confirm("신고하시겠습니까?");
    if (!isFired) {
      return;
    }

    // AJAX 요청으로 댓글 신고 처리
    $.ajax({
      url: "/users/board/comment/" + forReportCommentId + "/report",
      method: "POST",
      data: JSON.stringify({ forReportCommentId: forReportCommentId }),
      contentType: "application/json",
      success: function (res) {
        alert("신고 완료");
        window.location.reload();
      },
      error: handleError, // error 처리 함수 호출
    });
  });
});