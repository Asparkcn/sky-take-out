package com.sky.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.sky.dto.EmployeeDTO;
import com.sky.dto.EmployeeLoginDTO;
import com.sky.dto.PageQueryDTO;
import com.sky.dto.PasswordEditDTO;
import com.sky.entity.Employee;

public interface EmployeeService extends IService<Employee> {

    /**
     * 员工登录
     */
    Employee login(EmployeeLoginDTO employeeLoginDTO);

    void saveEmp(EmployeeDTO employeeDTO);

    void updateEmp(EmployeeDTO employeeDTO);

    Page<Employee> page(PageQueryDTO pageQueryDTO);

    void update(Long id, Integer status);

    void editPassword(PasswordEditDTO passwordEditDTO);
}
