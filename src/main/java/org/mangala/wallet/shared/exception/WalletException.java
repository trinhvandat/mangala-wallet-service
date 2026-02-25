package org.mangala.wallet.shared.exception;

import org.mangala.exception.BaseException;
import org.mangala.exception.ErrorDefinition;

public class WalletException extends BaseException {

    public WalletException(ErrorDefinition errorDefinition) {
        super(errorDefinition);
    }
}
