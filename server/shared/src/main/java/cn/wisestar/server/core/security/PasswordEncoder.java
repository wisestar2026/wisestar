package cn.wisestar.server.core.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 密码编码器（PasswordEncoder）。
 *
 * <p><b>所属模块</b>：shared 模块核心安全框架（cn.wisestar.server.core.security）。</p>
 * <p><b>类职责</b>：继承 Spring Security 的 {@link BCryptPasswordEncoder}，
 * 并将其注册为 Spring 容器中的 @Component，用于系统内所有密码的
 * <em>加密存储</em> 与 <em>校验比对</em>。</p>
 *
 * <p><b>使用的算法</b>：BCrypt（自带随机盐、单向不可逆、同密码多次加密结果不同），
 * 是业界推荐的密码散列方案，可有效抵御彩虹表攻击与暴力破解。</p>
 *
 * <p><b>使用场景与调用方</b>：</p>
 * <ul>
 *   <li>用户注册/创建用户时调用 {@code encode(明文)} 加密密码后落库（见 UserService 实现）；</li>
 *   <li>登录认证时由 {@link WebSecurityConfig#configure(AuthenticationManagerBuilder)}
 *       配置到 Spring Security 的 DaoAuthenticationProvider 中，自动调用
 *       {@code matches(明文, 密文)} 校验用户提交的密码；</li>
 *   <li>修改密码等场景校验旧密码。</li>
 * </ul>
 *
 * <p><b>为什么自定义子类</b>：直接以 @Component 注入容器，使业务代码可以
 * 通过构造器注入本类型，避免与 Spring Security 内部使用的编码器实例混淆。</p>
 *
 * @author javahuang
 * @date 2021/8/24
 */
@Component
public class PasswordEncoder extends BCryptPasswordEncoder {

}
