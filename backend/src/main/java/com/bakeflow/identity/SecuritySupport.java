package com.bakeflow.identity;
import java.util.*;import org.springframework.security.core.Authentication;import org.springframework.security.core.context.SecurityContextHolder;
public final class SecuritySupport{private SecuritySupport(){}public static UUID currentUserId(){Authentication a=SecurityContextHolder.getContext().getAuthentication();if(a==null||!a.isAuthenticated()||"anonymousUser".equals(a.getPrincipal()))return null;try{return UUID.fromString(a.getName());}catch(Exception e){return null;}}}
