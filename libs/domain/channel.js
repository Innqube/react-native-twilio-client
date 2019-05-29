/**
 * @author Enrique Viard.
 *         Copyright © 2019 InnQube. All rights reserved.
 */
class Channel {
    constructor(props) {
        this.sid = props.sid;
        this.friendlyName = props.friendlyName;
        this.uniqueName = props.uniqueName;
    }
}

export default Channel;