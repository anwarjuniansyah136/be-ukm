package id.pasim.mengaji.web;
import id.pasim.mengaji.domain.UserAccount;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.UUID;
public final class CurrentUser {
 static UserAccount get(HttpServletRequest request){Object u=request.getAttribute("currentUser");if(u instanceof UserAccount a)return a;throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);}
 public static void require(UserAccount u,id.pasim.mengaji.domain.Role... roles){for(var role:roles)if(u.getRole()==role)return;throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Tidak memiliki hak akses");}
 static UUID uuid(String s){try{return UUID.fromString(s);}catch(Exception ex){throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);}}
 private CurrentUser(){}
}
