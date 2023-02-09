package com.durgesh.Impl;

import com.durgesh.dto.CourseDto;
import com.durgesh.entity.Course;
import com.durgesh.service.CourseService;
import org.springframework.stereotype.Service;

@Service
public class CourseImpl implements CourseService {
    @Override
    public Course save(CourseDto courseDto) {
        return null;
    }

    @Override
    public Course getAll() {
        return null;
    }

    @Override
    public Course getById(Integer id) {
        return null;
    }

    @Override
    public Course removeAll() {
        return null;
    }

    @Override
    public Course removeById(Integer id) {
        return null;
    }
}
