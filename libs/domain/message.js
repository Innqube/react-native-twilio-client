/**
 * @author Enrique Viard.
 *         Copyright © 2019 InnQube. All rights reserved.
 */

class Message {
    constructor(props) {
        this.sid = props.sid;
        this.author = props.author;
        this.timeStamp = props.timeStamp;
        this.body = props.body;
    }
}

export default Message;