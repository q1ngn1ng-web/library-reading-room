package com.example.exception;

import com.example.common.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;

@ControllerAdvice(basePackages="com.example.controller")
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    //统一异常处理@ExceptionHandler,主要用于Exception
    @ExceptionHandler(Exception.class)
    @ResponseBody
    public Result error(HttpServletRequest request, Exception e){
        log.error("异常信息：",e);
        return Result.error("GlobalExeptionHandle表示：系统异常");
    }

    @ExceptionHandler(CustomException.class)
    @ResponseBody
    public Result customError(HttpServletRequest request, CustomException e){
        return Result.error(e.getMsg());
    }

    /** 请求体字段校验失败（@Valid）：返回第一条校验消息，而非"系统异常" */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseBody
    public Result validError(HttpServletRequest request, MethodArgumentNotValidException e){
        String msg = e.getBindingResult().getFieldErrors().isEmpty()
                ? "参数校验失败"
                : e.getBindingResult().getFieldErrors().get(0).getDefaultMessage();
        return Result.error(msg);
    }

    /** 缺少必填请求参数（如登录的 key）：友好提示 */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseBody
    public Result missingParam(HttpServletRequest request, MissingServletRequestParameterException e){
        return Result.error("缺少必填参数：" + e.getParameterName());
    }
}
