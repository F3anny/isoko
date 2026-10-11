package rw.ac.rca.isoko.identity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
public record LoginRequest(@NotBlank @Pattern(regexp="(^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$)|(^07[0-9]{8}$)",message="enter an email or Rwandan mobile number") String email,@NotBlank String password) {}
