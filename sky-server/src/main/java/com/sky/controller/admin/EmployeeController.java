package com.sky.controller.admin;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sky.constant.JwtClaimsConstant;
import com.sky.dto.EmployeeDTO;
import com.sky.dto.EmployeeLoginDTO;
import com.sky.dto.PageQueryDTO;
import com.sky.dto.PasswordEditDTO;
import com.sky.entity.Employee;
import com.sky.properties.JwtProperties;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.EmployeeService;
import com.sky.utils.JwtUtil;
import com.sky.vo.EmployeeLoginVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 员工管理
 */
@RestController
@RequestMapping("/admin/employee")
@Slf4j
@RequiredArgsConstructor
@Api(tags = "员工管理")
public class EmployeeController {

    private final EmployeeService employeeService;
    private final JwtProperties jwtProperties;

    @PostMapping("/login")
    @ApiOperation("用户登录")
    public Result<EmployeeLoginVO> login(@RequestBody EmployeeLoginDTO employeeLoginDTO) {
        log.info("员工登录：{}", employeeLoginDTO);

        Employee employee = employeeService.login(employeeLoginDTO);

        //登录成功后，生成jwt令牌
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.EMP_ID, employee.getId());
        String token = JwtUtil.createJWT(
                jwtProperties.getAdminSecretKey(),
                jwtProperties.getAdminTtl(),
                claims);


        EmployeeLoginVO employeeLoginVO = BeanUtil.copyProperties(employee, EmployeeLoginVO.class);
        employeeLoginVO.setToken(token);
        log.info("用户登录: {}", employeeLoginVO);
        return Result.success(employeeLoginVO);
    }


    @PostMapping("/logout")
    @ApiOperation("用户退出登录")
    public Result<String> logout() {
        return Result.success();
    }


    @PostMapping
    @ApiOperation("新增员工信息")
    public Result<String> saveEmp(@RequestBody EmployeeDTO employeeDTO) {
        employeeService.saveEmp(employeeDTO);
        log.info("新增员工: {}", employeeDTO);
        return Result.success();
    }

    @GetMapping("/{id}")
    @ApiOperation("根据 id 查询员工信息")
    public Result<Employee> getEmp(@PathVariable Long id) {
        Employee employee = employeeService.getById(id);
        log.info("根据 id 查询员工信息: {}", employee);
        return Result.success(employee);
    }

    @PutMapping
    @ApiOperation("更新员工信息")
    public Result<String> updateEmp(@RequestBody EmployeeDTO employeeDTO) {
        log.info("更新员工信息: {}", employeeDTO);
        employeeService.updateEmp(employeeDTO);
        return Result.success();
    }


    @PostMapping("/status/{status}")
    @ApiOperation("更新员工状态")
    public Result<String> updateEmpStatus(@RequestParam Long id, @PathVariable Integer status) {
        log.info("更新员工状态: {}", status);
        employeeService.update(id, status);
        return Result.success();
    }

    @GetMapping("/page")
    @ApiOperation("员工信息分页查询")
    public Result<PageResult<Employee>> pageQuery(PageQueryDTO pageQueryDTO) {
        Page<Employee> page = employeeService.page(pageQueryDTO);
        return Result.success(new PageResult<>(page.getTotal(), page.getRecords()));
    }

    @PutMapping("/editPassword")
    public Result<String> editPassword(@RequestBody PasswordEditDTO passwordEditDTO) {
        employeeService.editPassword(passwordEditDTO);
        return Result.success();
    }
}