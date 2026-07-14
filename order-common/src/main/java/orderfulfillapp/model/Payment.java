package orderfulfillapp.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Payment information for an order.
 */
public class Payment {

    @JsonProperty("creditCard")
    private CreditCard creditCard;

    public Payment() {
    }

    public Payment(CreditCard creditCard) {
        this.creditCard = creditCard;
    }

    public CreditCard getCreditCard() {
        return creditCard;
    }

    public void setCreditCard(CreditCard creditCard) {
        this.creditCard = creditCard;
    }

    @Override
    public String toString() {
        return "Payment{creditCard=" + creditCard + '}';
    }
}
