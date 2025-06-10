    package itu.mg.erp.exception;

    import java.io.PrintWriter;
    import java.io.StringWriter;
    
    import org.springframework.web.bind.annotation.ControllerAdvice;
    import org.springframework.web.bind.annotation.ExceptionHandler;
    import org.springframework.web.servlet.mvc.support.RedirectAttributes;

    @ControllerAdvice
    public class GlobalExceptionHandler {

        @ExceptionHandler(ApiException.class)
        public String handleApiException(ApiException ex, RedirectAttributes redirectAttributes) {
            // Ajout de logs pour déboguer
            System.out.println("ApiException caught!");
            System.out.println("Code: " + ex.getErrorCode());
            System.out.println("Message: " + ex.getMessage());

            redirectAttributes.addFlashAttribute("code", ex.getErrorCode());
            redirectAttributes.addFlashAttribute("messager", ex.getMessage());
            redirectAttributes.addFlashAttribute("excType", ex.getExecType());

            return "redirect:/error";
        }

        @ExceptionHandler(Exception.class)
        public String handleException(Exception ex, RedirectAttributes redirectAttributes) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            ex.printStackTrace(pw);  
            String stackTrace = sw.toString();
            redirectAttributes.addFlashAttribute("code", 500);
            redirectAttributes.addFlashAttribute("messager", ex.getMessage());
            redirectAttributes.addFlashAttribute("excType", stackTrace);
            return "redirect:/error";
        }

    }