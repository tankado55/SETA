package beans;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class LongBeanWrapper {
    private long wrapped;

    public LongBeanWrapper() {
    }

    public LongBeanWrapper(long wrapped) {
        this.wrapped = wrapped;
    }

    public long getWrapped() {
        return wrapped;
    }

    public void setWrapped(long wrapped) {
        this.wrapped = wrapped;
    }
}
