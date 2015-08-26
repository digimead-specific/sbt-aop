package sample;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;

@Aspect
public class SampleAspect {
  @Pointcut("execution(* add(..))")
  public void addExec() {}

  @Pointcut("execution(* addTest(..))")
  public void addTestExec() {}

  @Before("execution(* sample.Sample.printSample(..))")
  public void printSample() {
    System.out.println("Printing sample:");
  }

  @Around("addExec()")
  public Object addExecAdv(ProceedingJoinPoint pjp) throws Throwable {
    return ((Integer)pjp.proceed()) + 1;
  }
  @Around("addTestExec()")
  public Object addTestExecAdv(ProceedingJoinPoint pjp) throws Throwable {
    return ((Integer)pjp.proceed()) - 1;
  }
}
