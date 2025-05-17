package kh.link_up.mapper;

import kh.link_up.domain.Users;
import kh.link_up.dto.UsersDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface UserMapper {

    // 전체 사용자 조회 메서드 추가
    @Select("SELECT * FROM users")
    List<Users> getAllUsers();

    @Update("UPDATE users SET u_pwd = #{u_pwd} WHERE u_id = #{id}")
    void updateUserPwd(Users users);

    @Select("SELECT * FROM Users WHERE u_role != 'ADMIN'")
    List<UsersDTO> getAllUsersFromAdmin();

}
