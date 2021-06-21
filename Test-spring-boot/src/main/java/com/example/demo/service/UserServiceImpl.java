package com.example.demo.service;

import com.example.demo.entity.User;
import com.example.demo.exception.NotFoundException;
import com.example.demo.model.dto.UserDto;
import com.example.demo.model.mapper.UserMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class UserServiceImpl implements UserService {
    private static ArrayList<User> users=new ArrayList<User>();
    static {
        users.add(new User(1,"Thai","thaimeo1131@gmail.com","123 Son Son","123"));
        users.add(new User(2,"Dao","meo1@gmail.com","95 Son Tay","123"));
        users.add(new User(3,"Ninh","tamo11@gmail.com","12 Son A","123"));
        users.add(new User(4,"Xuyen","taeo3@gmail.com","96 Tay B","123"));

    }

    @Override
    public List<UserDto> getListUsers() {
        List<UserDto> result = new ArrayList<>();
        for (User user:users){
            result.add(UserMapper.toUserDto(user));
        }
        return result;
    }

    @Override
    public UserDto getUserById(int id) {
        for(User user:users){
            if(user.getId()==id){
                return UserMapper.toUserDto(user);
            }
        }
        throw new NotFoundException("User đéo tồn tại >< !");
    }

    @Override
    public List<UserDto> searchUser(String keyword) {
        List<UserDto> result= new ArrayList<>();
        for(User user:users){
            if(user.getName().contains(keyword)){
                result.add(UserMapper.toUserDto(user));
            }
        }
        return result;
    }

}
