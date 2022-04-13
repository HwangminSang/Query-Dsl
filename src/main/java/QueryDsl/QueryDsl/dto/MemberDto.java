package QueryDsl.QueryDsl.dto;


import com.querydsl.core.annotations.QueryProjection;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;


@NoArgsConstructor //기본생성자
@Data //toString 있음
public class MemberDto {

    //DTO도 Q파일로 생성 ->  other의 compileQuryDsl 해줘야 한다
    @QueryProjection
    public MemberDto(String username, int age) {
        this.username = username;
        this.age = age;
    }


    private String username;
    private int age;
}
