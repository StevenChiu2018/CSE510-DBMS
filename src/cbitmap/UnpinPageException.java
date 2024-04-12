package cbitmap;

import chainexception.*;

public class UnpinPageException extends ChainException {
  public UnpinPageException() {
    super();
  }

  public UnpinPageException(String s) {
    super(null, s);
  }

  public UnpinPageException(Exception e, String s) {
    super(e, s);
  }
}
