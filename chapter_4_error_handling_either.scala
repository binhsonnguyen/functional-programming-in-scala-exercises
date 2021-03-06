/*
 * Ý tưởng ở chương 4 là chúng ta có thể biểu diễn lại những lỗi và ngoại lệ bằng những giá trị
 * thông thường, và viết những hàng trừu tượng hoá những cách cứu lỗi mà bình thường chúng ta
 * vẫn áp dụng. Để làm việc đó, Maybe, không những dùng được, mà còn được dùng rất phổ biến,
 * nó thật sự quá đơn giản. Một điều chúng ta có thể phát hiện dc với Maybe đó là nó không cho
 * chúng ta biết bất cứ điều gì về "lỗi gì đã xảy ra". Thứ duy nhất nó cung câp được là giá trị
 * None, biểu thị rằng không tính được giá trị nào cả. Đôi khi chỉ nhiêu đấy là không đủ. Đơn
 * cử, ta muốn có thêm một String cho chúng ta biết thêm thông tin, rằng lỗi thật sự là gì.
 *
 * Chúng ta có thể tạo ra một kiểu dữ liệu mà có thể ghi lại bất cứ thông tin nào chúng ta muốn
 * về những lỗi đã xảy đến. Đôi khi ta chỉ cần biết là có lỗi là đủ - ta cũng đã có Maybe cho
 * trường hợp này, đôi khi ta muốn nhiều thông tin hơn. Trong phần này chúng ta sẽ lướt qua một
 * mở rộng đơn giản của Maybe - kiểu Either - thứ sẽ giúp ta tracking nguồn gốc của lỗi
 */
sealed trait Either[+E, +A] {

  /*
   * EXCERCISE 4.6
   *
   * Triển khai map, flatMap, orElse, map2
   */
  def map[B](f: A => B): Either[E, B] = this match {
    case Right(a) => Right(f(a))
    case Left(e) => Left(e)
  }

  def flatMap[EE >: E, B](f: A => Either[EE, B]): Either[EE, B] = this match {
    case Right(a) => f(a)
    case Left(e) => Left(e)
  }

  def getOrElse[AA >: A](v: => AA): AA = this match {
    case Right(a) => a
    case _ => v
  }

  def orElse[EE >: E, AA >: A](v: => Either[EE, AA]): Either[EE, AA] = this match {
    case Left(_) => v
    case _ => this
  }

  def map2[EE >: E, B, C](eb: Either[EE, B])(f: (A, B) => C): Either[EE, C] =
    this flatMap (a => eb map (f(a, _)))

  def map2ViaForComprehension[EE >: E, B, C](eb: Either[EE, B])(f: (A, B) => C): Either[EE, C] =
    for {
      a <- this
      b <- eb
    } yield f(a, b)
}

case class Left[+E](value: E) extends Either[E, Nothing]

case class Right[+A](value: A) extends Either[Nothing, A]

/*
 * Either chỉ có hai loại - giống như Maybe. Điểm khác nhau cốt lõi là cả hai loại đều có mang
 * giá trị. Kiểu Either biểu diễn, theo một cách rất chung, những giá trị mà chỉ thuộc vào một
 * trong hai loại. Ta có thể xem Either như là phép hợp không loại trừ của 2 kiểu dữ liệu. a
 * dùng nó để xác định rằng có lỗi hay không, bằng quy ước rằng kiểu Right là được chuẩn bị cho
 * trường hợp pass, và Left được sử dụng cho lỗi.
 *
 * Thử xem lại hàm `mean` sẽ trông như thế nào với Either
 */
def mean(xs: Seq[Double]): Either[String, Double] =
  if (xs.isEmpty)
    Left("mean of empty list")
  else
    Right(xs.sum / xs.length)

/*
 * Đôi khi ta có thể muốn bổ sung thêm nhiều thông tin hơn về lỗi, thay vì chỉ là một câu mô tả
 * đơn giản, điển hình nhất là một stack trace cho thấy vị trí của lỗi trong source code. Với
 * Either việc đó được thực hiện rất đơn giản:
 */
def safeDiv(x: Int, y: Int): Either[Exception, Int] =
  try Right(x / y)
  catch {
    case e: Exception => Left(e)
  }

/*
 * Cũng như với Maybe, ta dễ dàng triển khai hàm Try để chuyển một hàm có throw Exception thành
 * hàm return Either
 */
def Try[A](f: => A): Either[Exception, A] =
  try Right(f)
  catch {
    case e: Exception => Left(e)
  }

/*
 * EXCERCISE 4.7
 * Triển khai `sequence` và `traverse`. Kết quả của chúng sẽ có lỗi đầu tiên gặp được
 * nếu có.
 */
def sequence[E, A](es: List[Either[E, A]]): Either[E, List[A]] =
  es.foldRight[Either[E, List[A]]](Right(Nil))((ea, eas) => (ea map2 eas) (_ :: _))

def traverse[E, A, B](as: List[A])(f: A => Either[E, B]): Either[E, List[B]] =
  as.foldRight[Either[E, List[B]]](Right(Nil))((a, ebs) => f(a).map2(ebs)(_ :: _))

def sequenceViaTraverse[E, A](es: List[Either[E, A]]): Either[E, List[A]] =
  traverse(es)(e => e)

/*
 * Demo
 */
case class Person(name: Name, age: Age)

sealed class Name(val value: String)

sealed class Age(val value: Int)

def makeName(name: String): Either[String, Name] =
  if (name == null || name.isEmpty) Left("empty name!")
  else Right(new Name(name))

def makeAge(age: Int): Either[String, Age] =
  if (age < 0) Left("age out of range!")
  else Right(new Age(age))

def makePerson(name: String, age: Int): Either[String, Person] =
  makeName(name).map2(makeAge(age))(Person)

/*
 * EXERCISE 4.8
 *
 * Trong demo trên, `map2` chỉ có khả năng báo cáo một lỗi, kể cả khi cả name lẫn age đều invalid.
 * Chúng ta cần gì để có thể báo cáo được cả hai lỗi? Sửa map2 hay sửa nguyên mẫu của makePerson?
 * Hay là phải tạo ra một kiểu dữ liệu mới để phục vụ yêu cầu đó tốt hơn so với Either? Những
 * orElse, traverse, sequence đối xử như thế nào với kiểu dữ liệu đó?
 *
 * ANS: có thể dùng Either[List[E], A], hoặc dùng một kiểu mới, như dưới đây
 */
sealed trait Partial[+A, +B]

case class Errors[+A](get: Seq[A]) extends Partial[A, Nothing]

case class Success[B](get: B) extends Partial[Nothing, B]

/*
 * Note:
 *
 * Suốt chương này chúng ta đã phân tích những vấn đề của `Exception` và làm quen với những nguyên
 * tắc xử lý lỗi của lập trình thuần hàm. Mặc dù chúng ta đã tập trung vào những kiểu đại số là
 * `Maybe` và `Either`, nhưng phải nhớ rằng ý tưởng lớn hơn ở đây đó là chúng ta có thể biểu diễn
 * lại lỗi như là các giá trị thông thường và sử dụng HOF để đóng gói những mẫu thiết kế phổ biến
 * của xử lý lỗi. Ý tưởng chung nhất này, về việc biểu diễn lại hiệu ứng phụ như những giá trị,
 * là cái gì đó mà chúng ta sẽ gặp đi gặp lại suốt cuốn sách.
 *
 * Chúng tôi không đặt kỳ vọng bạn sẽ lưu loát mọi HOF mà chúng tôi viết, nhưng ít nhất bây giờ
 * bạn đã phải thấy quen thuộc với việc viết mã cho hàm của bạn với những xử lý lỗi được viết
 * hoàn toàn bằng lập trình hàm. Với những công cụ đã được cung cấp, giờ đây Exception chỉ được
 * dành riêng cho những lỗi thật sự không thể cứu được.
 *
 * Điều cuối cùng, là trong chương vừa rồi chúng ta đã chạm một ít vào khái niệm hàm
 * non-strict: `orElse`, `getOrElse` and `Try`. Trong chương sau chúng ta sẽ tìm hiểu tại sao
 * non-strict lại quan trọng, và tại sao chúng có thể cho chúng ta nhiều tính module và hiệu quả
 * hơn trong chương trình của chúng ta.
 */
