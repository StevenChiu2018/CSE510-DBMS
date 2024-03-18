package bitmap;

import chainexception.*;

public class AddFileEntryException extends ChainException {
  public AddFileEntryException() {
    super();
  }

  public AddFileEntryException(String s) {
    super(null, s);
  }

  public AddFileEntryException(Exception e, String s) {
    super(e, s);
  }
}
