$(document).ready(function () {
    // CSRF 토큰을 가져오기
    const csrfToken = document.querySelector('meta[name="_csrf"]').getAttribute('content');
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');

    // CKEditor 초기화
    let editor;
    ClassicEditor
        .create(document.querySelector('#editContent'), {
            ckfinder: {
                uploadUrl: '/notion/image'  // 이미지 업로드 요청을 처리하는 백엔드 URL
            },
            mediaEmbed: {
                previewsInData: true  // 미디어 임베드 지원
            }
        })
        .then(newEditor => {
            editor = newEditor;  // 에디터 인스턴스 저장

            // 업로드 어댑터에 CSRF 토큰 추가
            editor.plugins.get('FileRepository').createUploadAdapter = (loader) => {
                return {
                    upload: () => {
                        return loader.file
                            .then(file => new Promise((resolve, reject) => {
                                const formData = new FormData();
                                formData.append('file', file);

                                $.ajax({
                                    url: '/notion/image',
                                    type: 'POST',
                                    headers: {
                                        [csrfHeader]: csrfToken  // CSRF 토큰 추가
                                    },
                                    data: formData,
                                    processData: false,  // FormData를 처리할 수 있도록 설정
                                    contentType: false,  // content-type 헤더 자동으로 처리되지 않도록 설정
                                    success: (response) => {
                                        const imageFileName = JSON.parse(response);
                                        const filename = imageFileName.filename
                                        const fileUrl = '/notion/images/' + filename;  // 서버에서 반환한 파일 이름을 사용하여 URL 생성
                                        resolve({
                                            default: fileUrl
                                        });
                                    },
                                    error: (xhr) => {
                                        reject(`업로드 실패: ${xhr.status}, ${xhr.responseText}`);
                                    }
                                });
                            }));
                    }
                };
            };
    
            // 이미지 삭제 감지를 위해 change:data 이벤트를 감시
            editor.model.document.on('change:data', () => {
                const editorData = editor.getData(); // 에디터의 현재 HTML 데이터를 가져옴

                // 현재 에디터에서 사용 중인 이미지 목록을 추출
                const imgTags = editorData.match(/<img[^>]+src="([^">]+)"/g);
                const currentImageUrls = imgTags ? imgTags.map(imgTag => imgTag.match(/src="([^">]+)"/)[1]) : [];

                // 이전에 저장한 이미지 목록과 비교하여 삭제된 이미지 찾기
                const deletedImages = previousImages.filter(imgUrl => !currentImageUrls.includes(imgUrl));

                // 삭제된 이미지 서버로 전송
                deletedImages.forEach(imageUrl => {
                    const filename = imageUrl.split('/').pop();  // 파일명 추출
                    deleteImageOnServer(filename);  // 서버에 삭제 요청
                });

                // 이전 이미지 목록 업데이트
                previousImages = currentImageUrls;
            });

            // 서버에 이미지 삭제 요청 함수
            function deleteImageOnServer(filename) {
                const formData = new FormData();
                formData.append('filename', filename);

                $.ajax({
                    url: '/notion/delete-image',
                    type: 'POST',
                    headers: {
                        [csrfHeader]: csrfToken  // CSRF 토큰 추가
                    },
                    data: formData,
                    processData: false,
                    contentType: false,
                    success: function (response) {
                        console.log('이미지 삭제 성공');
                    },
                    error: function (xhr) {
                        console.error('이미지 삭제 실패:', xhr.responseText);
                    }
                });
            }

            // 처음 로드된 에디터 데이터에 포함된 이미지 URL을 저장
            let previousImages = [];
            editor.model.document.once('change:data', () => {
                const initialEditorData = editor.getData();
                const initialImgTags = initialEditorData.match(/<img[^>]+src="([^">]+)"/g);
                previousImages = initialImgTags ? initialImgTags.map(imgTag => imgTag.match(/src="([^">]+)"/)[1]) : [];
            });
        })
        .catch(error => {
            console.error('CKEditor 실행 실패:', error);
        });

    // 새 노션 버튼 클릭 이벤트
    $('#newNotionBtn').on('click', function () {
        clearNotionEditor();
    });

    // 새 노션 버튼 클릭 이벤트
    $('#cancelBtn').on('click', function () {
        clearNotionEditor();
    });

    // 노션 저장 버튼 클릭 이벤트
    $('#saveBtn').on('click', function () {
        const uIdx = $(this).attr('data-uidx');
        const title = $('#editTitle').val();
        const content = editor.getData(); // CKEditor에서 값 가져오기
        const postTitle = $('#postTitle').val(); // 수정 시 nIdx 값이 들어옴

        if (!title || !content) {
            console.log(title + " / " + content)
            alert('제목과 내용을 입력하세요.');
            return;
        }

        const notionData = {
            n_title: title,
            n_content: content,
            n_upload: new Date().toISOString() // 현재 시간을 ISO 8601 형식으로 넣기
        };

        if (postTitle) {
            // 수정 요청 (nIdx가 있을 때)
            updateNotion(uIdx, postTitle, notionData);
        } else {
            // 새 노션 저장
            saveNotion(uIdx, notionData);
        }
    });

    // 노션 삭제 버튼 클릭 이벤트
    $('#deleteNotionBtn').on('click', function () {
        const nIdx = $(this).attr('data-id');
        deleteNotion(nIdx);
    });

    // 노션 저장 함수 (새로 추가)
    function saveNotion(uIdx, notionData) {
        $.ajax({
            url: `/users/${uIdx}/notion`,
            type: 'POST',
            contentType: 'application/json; charset=utf-8',  // JSON 형식으로 전송
            data: JSON.stringify(notionData),  // notionData 객체를 JSON으로 변환하여 전송
            beforeSend: function (xhr) {
                xhr.setRequestHeader(csrfHeader, csrfToken);
            },
            success: function (response) {
                alert('노션 저장 완료');
                location.reload(); // 페이지 새로고침
            },
            error: function (xhr) {
                alert('노션 저장 실패: ' + xhr.responseText);
            }
        });
    }

    // 노션 수정 함수
    function updateNotion(uIdx, nIdx, notionData) {
        $.ajax({
            url: `/users/${uIdx}/notion/${nIdx}`,
            type: 'PUT',
            data: JSON.stringify(notionData),
            contentType: 'application/json; charset=utf-8',
            beforeSend: function (xhr) {
                xhr.setRequestHeader(csrfHeader, csrfToken);
            },
            success: function (response) {
                alert('노션 수정 완료');
                location.reload(); // 페이지 새로고침
            },
            error: function (xhr) {
                alert('노션 수정 실패: ' + xhr.responseText);
            }
        });
    }

    // 노션 제목 클릭 시 해당 노션 데이터 가져오기
    $('.notion-link').on('click', function () {
        const nIdx = $(this).data('id'); // 클릭한 노션의 n_idx 값을 가져옴
        getNotion(nIdx); // 해당 n_idx로 getNotion 호출
    });

    // 노션 삭제 함수
    function deleteNotion(nIdx) {
        if (!confirm('정말로 삭제하시겠습니까?')) return;

        $.ajax({
            url: `/notion/${nIdx}`,
            type: 'DELETE',
            beforeSend: function (xhr) {
                xhr.setRequestHeader(csrfHeader, csrfToken);
            },
            success: function () {
                alert('노션 삭제 완료');
                location.reload(); // 페이지 새로고침
            },
            error: function (xhr) {
                alert('노션 삭제 실패: ' + xhr.responseText);
            }
        });
    }

    // 에디터 초기화 함수
    function clearNotionEditor() {
        $('#editTitle').val('');
        editor.setData('');
        $('#postTitle').val(''); // 새 노션일 경우 빈 값으로 설정
    }

    // 노션 데이터 가져오기
    function getNotion(nIdx) {
        $.ajax({
            url: `/notion/${nIdx}`,
            type: 'GET',
            success: function (response) {
                $('#editTitle').val(response.n_title);
                // CKEditor 에디터에 컨텐츠 설정
                if (editor) {
                    editor.setData(response.n_content); // CKEditor에 값 설정
                } else {
                    alert('에디터가 초기화되지 않았습니다.');
                }
                $('#postTitle').val(response.n_idx); // 수정 시 사용하기 위해 n_idx 저장
            },
            error: function (xhr) {
                alert('노션 가져오기 실패: ' + xhr.responseText);
            }
        });
    }
});