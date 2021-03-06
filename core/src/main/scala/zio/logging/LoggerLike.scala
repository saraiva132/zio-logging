package zio.logging
import zio.{ UIO, URIO, ZIO }

trait LoggerLike[-A] { self =>

  /**
   * Produces a new logger by adapting a different input type to the input
   * type of this logger.
   */
  final def contramap[A1](f: A1 => A): LoggerLike[A1] =
    new LoggerLike[A1] {
      def locally[R1, E, A2](f: LogContext => LogContext)(zio: ZIO[R1, E, A2]): ZIO[R1, E, A2] =
        self.locally(f)(zio)

      def log(line: => A1): UIO[Unit] = self.log(f(line))

      def logContext: UIO[LogContext] = self.logContext
    }

  /**
   * Derives a new logger from this one, by applying the specified decorator
   * to the logger context.
   */
  def derive(f: LogContext => LogContext): LoggerLike[A] =
    new LoggerLike[A] {
      def locally[R1, E, A1](f: LogContext => LogContext)(zio: ZIO[R1, E, A1]): ZIO[R1, E, A1] =
        self.locally(f)(zio)

      def log(line: => A): UIO[Unit] = locally(f)(self.log(line))

      def logContext: UIO[LogContext] = self.logContext
    }

  /**
   * Modifies the log context in the scope of the specified effect.
   */
  def locally[R1, E, A1](f: LogContext => LogContext)(zio: ZIO[R1, E, A1]): ZIO[R1, E, A1]

  /**
   * Modifies the log context with effect in the scope of the specified effect.
   */
  def locallyM[R1, E, A1](f: LogContext => URIO[R1, LogContext])(zio: ZIO[R1, E, A1]): ZIO[R1, E, A1] =
    logContext.flatMap(ctx => f(ctx)).flatMap(ctx => locally(_ => ctx)(zio))

  /**
   * Modifies the annotate in the scope of the specified effect.
   */
  final def locallyAnnotate[B, R, E, A1](annotation: LogAnnotation[B], value: B)(zio: ZIO[R, E, A1]): ZIO[R, E, A1] =
    locally(_.annotate(annotation, value))(zio)

  /**
   * Logs the specified element using an inherited log level.
   */
  def log(line: => A): UIO[Unit]

  /**
   * Retrieves the log context.
   */
  def logContext: UIO[LogContext]

  /**
   * Logs the specified element at the specified level. Implementations may
   * override this for greater efficiency.
   */
  def log(level: LogLevel)(line: => A): UIO[Unit] =
    locally(_.annotate(LogAnnotation.Level, level))(log(line))

  /**
   * Produces a named logger.
   */
  def named(name: String): LoggerLike[A] =
    derive(_.annotate(LogAnnotation.Name, name :: Nil))
}
