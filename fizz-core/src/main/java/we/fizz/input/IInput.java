package we.fizz.input;

import org.springframework.context.ConfigurableApplicationContext;
import reactor.core.publisher.Mono;
import we.fizz.Step;
import we.fizz.StepContext;
import we.fizz.StepResponse;

import java.lang.ref.SoftReference;
import java.lang.reflect.Field;
import java.util.Map;

public interface IInput {
    public static final InputType TYPE = null;
    public static Class inputConfigClass() {
        return null;
    }
    public String getName() ;
    public boolean needRun(StepContext<String, Object> stepContext);
    public void beforeRun(InputContext context);
    public Mono<Map> run();

    public StepResponse getStepResponse() ;
    public void setStepResponse(StepResponse stepResponse);
    public SoftReference<Step> getWeakStep();
    public void setWeakStep(SoftReference<Step> weakStep);

    public ConfigurableApplicationContext getCurrentApplicationContext();

}
