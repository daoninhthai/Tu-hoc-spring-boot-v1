package com.example.demo.service;

import com.example.demo.entity.User;
import com.example.demo.model.dto.UserDto;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface UserService {
    public List<UserDto> getListUsers();

    public UserDto getUserById(int id);

    public List<UserDto> searchUser(String keyword);
}
