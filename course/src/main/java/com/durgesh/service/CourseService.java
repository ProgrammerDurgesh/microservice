package com.durgesh.service;

import com.durgesh.dto.CourseDto;
import com.durgesh.entity.Course;

public interface CourseService {


    Course save(CourseDto courseDto);

    Course getAll();

    Course getById(Integer id);

    Course removeAll();

    Course removeById(Integer id);


}
