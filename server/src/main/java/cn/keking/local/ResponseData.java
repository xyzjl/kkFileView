package cn.keking.local;

import java.io.Serializable;

/**
 * 封装的接口返回数据
 * <p>
 * 版本: 1.0
 * <p>
 * 创建时间: 2019-04-09 10:08:00
 */
public class ResponseData implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String SUCCESS_CODE = "200";
    public static final String ERROR_CODE = "500";
    public static final String ERROR_WITH_DATA_CODE = "999";
    public static final String UNLOGIN_CODE = "304";
    public static final String NO_INFO_CODE = "305";
    public static final String UNAUTH_CODE = "401";
    public static final String NOT_FOUND_CODE = "404";

    String code;
    String msg;
    Object data;

    public static ResponseData success() {
        return new ResponseData(SUCCESS_CODE, "ok", null);
    }

    public static ResponseData success(Object data) {
        return new ResponseData(SUCCESS_CODE, "ok", data);
    }

    public static ResponseData error() {
        return new ResponseData(ERROR_CODE, "服务器出现错误!", null);
    }

    public static ResponseData error(String msg) {
        return new ResponseData(ERROR_CODE, msg, null);
    }

    public static ResponseData error(String msg, Object data) {
        return new ResponseData(ERROR_CODE, msg, data);
    }

    public static ResponseData errorWithData(String msg, Object data) {
        return new ResponseData(ERROR_WITH_DATA_CODE, msg, data);
    }

    public static ResponseData unLogin() {
        return new ResponseData(UNLOGIN_CODE, "请重新登陆!", null);
    }

    public static ResponseData noInfo() {
        return new ResponseData(NO_INFO_CODE, "请填写信息!", null);
    }

    public static ResponseData unAuth() {
        return new ResponseData(UNAUTH_CODE, "没有权限", null);
    }

    public static ResponseData notFound() {
        return new ResponseData(NOT_FOUND_CODE, "该资源不存在!", null);
    }

    public ResponseData() {
    }

    public ResponseData(String code, String msg, Object data) {
        super();
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "ResponseData [code=" + code + ", msg=" + msg + ", data=" + data + "]";
    }

}
