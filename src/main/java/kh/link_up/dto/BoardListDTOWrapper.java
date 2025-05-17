package kh.link_up.dto;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@JsonTypeInfo(
        use = JsonTypeInfo.Id.CLASS,
        include = JsonTypeInfo.As.PROPERTY,
        property = "@class"
)
public class BoardListDTOWrapper implements Serializable {
    private List<BoardListDTO> boardListDTO;

    public BoardListDTOWrapper(List<BoardListDTO> boardListDTO) {
        this.boardListDTO = boardListDTO;
    }
}

