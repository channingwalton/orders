package acme.orders.models

sealed trait ServiceError extends Throwable

object ServiceError:
  case class OrderNotFound(orderId: OrderId) extends ServiceError:
    override def getMessage: String = s"Order not found: ${orderId.value}"
  
  case class InvalidProduct(productId: ProductId) extends ServiceError:
    override def getMessage: String = s"Invalid product: ${productId.value}"
    
  case class DatabaseError(cause: Throwable) extends ServiceError:
    override def getMessage: String = s"Database error: ${cause.getMessage}"
    override def getCause: Throwable = cause