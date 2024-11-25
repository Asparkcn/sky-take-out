package com.sky.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sky.constant.MessageConstant;
import com.sky.constant.PasswordConstant;
import com.sky.constant.StatusConstant;
import com.sky.context.BaseContext;
import com.sky.dto.EmployeeDTO;
import com.sky.dto.EmployeeLoginDTO;
import com.sky.dto.PageQueryDTO;
import com.sky.dto.PasswordEditDTO;
import com.sky.entity.Employee;
import com.sky.exception.AccountLockedException;
import com.sky.exception.AccountNotFoundException;
import com.sky.exception.BaseException;
import com.sky.exception.PasswordErrorException;
import com.sky.mapper.EmployeeMapper;
import com.sky.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmployeeServiceImpl extends ServiceImpl<EmployeeMapper, Employee>
        implements EmployeeService {

    private final EmployeeMapper employeeMapper;

    /**
     * 员工登录
     */
    @Override
    public Employee login(EmployeeLoginDTO employeeLoginDTO) {
        String username = employeeLoginDTO.getUsername();
        String password = employeeLoginDTO.getPassword();
        //1、根据用户名查询数据库中的数据
        Employee employee = baseMapper.getByUsername(username);

        //2、处理各种异常情况（用户名不存在、密码不对、账号被锁定）
        if (employee == null) {
            //账号不存在
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }

        //密码比对
        String md5Passwd = SecureUtil.md5(password);
        log.info(md5Passwd);
        if (!md5Passwd.equals(employee.getPassword())) {
            //密码错误
            throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
        }

        if (employee.getStatus().equals(StatusConstant.DISABLE)) {
            //账号被锁定
            throw new AccountLockedException(MessageConstant.ACCOUNT_LOCKED);
        }

        //3、返回实体对象
        return employee;
    }

    @Override
    public void saveEmp(EmployeeDTO employeeDTO) {
        Employee employee = BeanUtil.copyProperties(employeeDTO, Employee.class);
        employee.setCreateTime(LocalDateTime.now());
        employee.setUpdateTime(LocalDateTime.now());
        employee.setCreateUser(BaseContext.getCurrentId());
        employee.setUpdateUser(BaseContext.getCurrentId());
        employee.setPassword(SecureUtil.md5(PasswordConstant.DEFAULT_PASSWORD));
        log.info("{}", employee);
        baseMapper.insert(employee);
    }

    @Override
    public void updateEmp(EmployeeDTO employeeDTO) {
        Employee employee = BeanUtil.copyProperties(employeeDTO, Employee.class);
        employee.setUpdateTime(LocalDateTime.now());
        employee.setUpdateUser(BaseContext.getCurrentId());
        baseMapper.insert(employee);
    }

    @Override
    public Page<Employee> page(PageQueryDTO pageQueryDTO) {

        pageQueryDTO.setPage(Math.max(pageQueryDTO.getPage(), 1));
        pageQueryDTO.setPageSize(Math.min(Math.max(pageQueryDTO.getPageSize(), 1), 20));

        Page<Employee> page = new Page<>(pageQueryDTO.getPage(), pageQueryDTO.getPageSize());

        LambdaQueryWrapper<Employee> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(pageQueryDTO.getName() != null, Employee::getName, pageQueryDTO.getName());

        return baseMapper.selectPage(page, wrapper);

    }

    @Override
    public void update(Long id, Integer status) {
        if (id == null || status == null) {
            throw new BaseException("id 或 status 不能为空");
        }
        LambdaQueryWrapper<Employee> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Employee::getId, id);
        Employee employee = baseMapper.selectOne(wrapper);
        employee.setStatus(status);
        employee.setUpdateTime(LocalDateTime.now());
        employee.setUpdateUser(BaseContext.getCurrentId());
        baseMapper.update(employee, wrapper);
    }

    @Override
    public void editPassword(PasswordEditDTO passwordEditDTO) {
        if (StringUtils.isEmpty(passwordEditDTO.getNewPassword())
                || StringUtils.isEmpty(passwordEditDTO.getOldPassword())
        ) {
            throw new BaseException("参数传递错误");
        }

        Employee employee = employeeMapper.selectById(BaseContext.getCurrentId());
        if (employee == null) {
            throw new BaseException(MessageConstant.ACCOUNT_NOT_FOUND);
        }

        String oldPasswordMd5 = SecureUtil.md5(passwordEditDTO.getOldPassword());
        if (!employee.getPassword().equals(oldPasswordMd5)) {
            throw new BaseException(MessageConstant.PASSWORD_ERROR);
        }

        employee.setPassword(SecureUtil.md5(passwordEditDTO.getNewPassword()));
        employeeMapper.updateById(employee);
    }


}
