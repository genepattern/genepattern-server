/*!
 * jQuery Post-Comment Plugin v.1.0
 * created [2013-02-26 17:49 AM]
 * TODO: Documentation
 *
 * Copyright 2013, hendra@kunchy.com or henyana@gmail.com
 * http://www.abelware.com
 */

// Utility
if ( typeof Object.create !== 'function' ) {
	Object.create = function( obj ) {
		function F() {};
		F.prototype = obj;
		return new F();
	};
}

(function($, window, document, undefined){

	var Comment = {
		init_: function( options, elem ) {
			var self = this;

			self.elem = elem;
			self.$elem = $( elem );

			self.options = $.extend( {}, $.fn.comment.options, options );

			self.refresh_( 1 );
		},

		buildForm_: function(comment_id, parent_id){
			var self = this;
			
			var form_elem = $('<form></form>');

			if(comment_id!=null)
				form_elem.attr('action', self.options.url_input+'/'+comment_id);
			else
				form_elem.attr('action', self.options.url_input);

			form_elem.attr('method', 'post');

			if(parent_id!=null)
			{
				var parent_id_field = $('<input/>');
				parent_id_field.attr('type', 'hidden');
				parent_id_field.attr('name', 'parent_id');
				parent_id_field.val(parent_id);
				form_elem.append(parent_id_field);
			}

			var textarea = $('<textarea></textarea>');
			textarea.attr('name', 'text');
			textarea.attr('placeHolder', 'Leave a message...');
			textarea.css('overflow', 'hidden');
			textarea.autogrow();

			textarea.on('keypress', function(e){
				e = e || event;
				if (e.keyCode === 13 && !e.shiftKey && $.trim(this.value).length>0) {
					e.preventDefault();
					//form_elem.submit();
					

					self.submitForm_(comment_id, form_elem.serialize());
				}
			});

			form_elem.append(textarea);

			return form_elem;
		},

		submitForm_: function(comment_id, form_data){
			var self = this;

			var url_input = self.options.url_input;

			if(comment_id!=null) 	// form edit mode
				url_input = self.options.url_input+'/'+comment_id;

			return $.ajax({
				url: url_input,
				data: form_data,
				type: 'post',
				dataType: 'json',
                beforeSend: function(xhr, opts){

                    $('textarea', self.$elem).attr("disabled", true);

                }
            }).done( function(result){

                if(result.success!=undefined)
                {
                    if(result.success===false)
                    {
                        // error
                        $.each(result, function(key, val){
                             // check error if any
                            if(val.error!=undefined)
                            {
                                $show_warning_(val.error);
                                return false;
                            }
                        });
                    }
                    else
                    {
                    	if(comment_id!=null)	// edit mode
                    	{
                    		var item = $('#posted-'+comment_id, self.$elem);
                    		
                    		var item_txt = $('.posted-comment-txt:hidden', item);
                    		item_txt.html(result.text);
                    		item_txt.toggle();

                    		var item_form_edit = $('.posted-comment-form-edit', item);
                    		item_form_edit.toggle();

                    	}
                    	else
                    	{
	                    	result.fullname = self.user_info_.fullname;
	                    	result.picture = self.user_info_.picture;

	                    	// add new itemlist
							var itemlist = self.buildItemList_( result );

							if(result.parent_id===undefined)
		                    	self.$rootlist.prepend(itemlist);
		                    else
		                    {
		                    	if(result.parent_id==0)
			                    	self.$rootlist.prepend(itemlist);
		                    	else
		                    	{
			                    	var id = 'posted-comment-child-'+result.parent_id;

			                    	//prepend the new comment
			                    	var the_child = $('ul[id="'+id+'"]', self.$elem).prepend(itemlist);

			                    	// hide the form post
			                    	$('div.posted-comments-postbox:visible', the_child).hide();
		                    	}
		                    }

	                    	// update total comment
	                    	self.total_comment++;
	                    	self.$total_comment.html(self.total_comment+' '+self.options.title);
                    	}

                    	// clear and enable textarea
	                    $('textarea', self.$elem).val('');                    	
	                    $('textarea', self.$elem).attr("disabled", false);                    	                    		
                    }
                }                   


            });
		},

		buildPostBox_: function(parent_id){
			var self = this;
			
			var elem = $('<div></div>');
			elem.addClass('posted-comments-postbox');

			//self.user_info_
			var img_elem = $('<img/>');
			img_elem.attr('src', self.user_info_.picture);
			img_elem.attr('border', 0);
			img_elem.addClass('ui-corner-all');
			img_elem.addClass('curr-user-photo');

			var avatar = $('<div></div>');
			avatar.addClass('avatar').addClass('pull-left');
			avatar.append(img_elem);
			
			elem.append(avatar);


			var form = $('<div></div>');
			form.addClass('form').addClass('pull-left');

			if(self.user_info_.is_add_allowed)
			{
				// form new
				var form_elem = self.buildForm_(null, parent_id);
				form.append(form_elem);				
			}

			elem.append(form);

			var clear = $('<div></div>');
			clear.addClass('clear');

			elem.append(clear);

			return elem;
		},

		buildUl_: function(){
			var self = this;
			
			var ul_elem = $('<ul></ul>');
			ul_elem.addClass('posted-comments');

			return ul_elem;
		},

		refresh_: function( length ) {
			var self = this;

			setTimeout(function() {
				self.fetch_().done(function( results ) {

					//console.log(results);

					// results['user']
					if(results.results.user!=undefined)
						self.user_info_ = results.results.user;

					// results['comments']
					if(results.results.comments!=undefined)
						results_ = results.results.comments;
					// results = self.limit_( results.results.comments, self.options.limit );

					// results['total_comment']
					if(results.results.total_comment!=undefined)
						self.total_comment = results.results.total_comment;

					self.buildList_( results_ );

					self.display_();

					if ( typeof self.options.onComplete === 'function' ) {
						self.options.onComplete.apply( self.elem, arguments );
					}

					if ( self.options.refresh && self.options.auto_refresh ) {
						self.refresh_();
					}
				});
			}, length || self.options.refresh );
		},

		fetch_: function() {
			var self = this;
			return $.ajax({
				url: self.options.url_get,
				dataType: 'json'
			});
		},

		buildList_: function( results ) {
			var self = this;
			self.comments = $.map( results, function( obj, i) {
				return self.buildItemList_( obj );
			});
		},

		buildItemList_: function( comment_info ) {
			var self = this;

			var item = $( self.options.wrapEachWith );

			item.attr('id', 'posted-'+comment_info.comment_id);

			// avatar-image
			var avatar = $('<div></div>');
			avatar.addClass('avatar').addClass('pull-left');

			var img_elem = $('<img/>');
			img_elem.attr('src', comment_info.picture);
			img_elem.attr('border', 0);
			img_elem.addClass('ui-corner-all');

			if(comment_info.created_by==self.user_info_.user_id)
				img_elem.addClass('curr-user-photo');

			avatar.append(img_elem);

			item.append(avatar);

			// posted-comment-container
			var post_container = $('<div></div>');
			post_container.addClass('posted-comment-container').addClass('pull-left');

			// posted-comment-head
			var post_head = $('<div></div>');
			post_head.addClass('posted-comment-head');

			// user-fullname
			var username = $('<span></span>');
			username.addClass('posted-comment-author');
			username.html(comment_info.fullname);
			
			post_head.append(username);

			// in reply-to
			if(comment_info.parent_id!=0)
			{
				// in-reply-to
				var in_reply_to = $('<span></span>');
				in_reply_to.addClass('in-reply-to');
				in_reply_to.attr('title', 'in reply-to');

				// arrow
				var arrow = $('<i></i>');
				arrow.addClass('ui-icon');
				arrow.addClass('ui-icon-arrow-1-e');

				in_reply_to.append(arrow);

				post_head.append(in_reply_to);

				// user-fullname reply
				var username_reply = $('<span></span>');
				username_reply.addClass('posted-comment-author-reply');
				username_reply.html(comment_info.in_reply_to);

				post_head.append(username_reply);
			}

			// dot
			var dot = $('<span></span>');
			dot.addClass('dot');
			dot.html('&bull;');

			post_head.append(dot);

			// posted time
			var posted_date = $('<span></span>');
			posted_date.addClass('real-time');
			posted_date.attr('title', self.timeStringToABBR_(comment_info.posted_date));
			posted_date.html(comment_info.posted_date);
			posted_date.timeago();

			post_head.append(posted_date);

			post_container.append(post_head);

			// posted-comment-body
			var post_body = $('<div></div>');
			post_body.addClass('posted-comment-body');
			
			// posted-comment-txt
			var post_txt = $('<div></div>');
			post_txt.addClass('posted-comment-txt');
			post_txt.html(comment_info.text);

			post_body.append(post_txt);
			
			post_container.append(post_body);

			// posted-comment-foot
			var post_foot = $('<div></div>');
			post_foot.addClass('posted-comment-foot');

			// edit
			if(self.user_info_.is_edit_allowed && (comment_info.created_by==self.user_info_.user_id))
			{
				// form edit
				var form_edit_container = $('<div></div>');
				form_edit_container.addClass('posted-comment-form-edit');
				form_edit_container.hide();
				var form_edit_elem = self.buildForm_(comment_info.comment_id, comment_info.parent_id);
				form_edit_container.append(form_edit_elem);
				
				post_body.append(form_edit_container);


				var edit_container = $('<span></span>');
				edit_container.addClass('post-edit');

				var edit = $('<a>Edit</a>');
				edit.attr('href','#');
				edit.attr('title','Edit');

				edit_container.append(edit);

				post_foot.append(edit_container);

				var dot = $('<span></span>');
				dot.addClass('dot');
				dot.html('&bull;');

				post_foot.append(dot);

				// edit events-apply
				edit.on('click', function(e){
					e.preventDefault();
					post_txt.toggle();

					form_edit_container.toggle();
					var textarea = $('textarea', form_edit_container);
					textarea.val(post_txt.html());
					textarea.autogrow();
					textarea.focus();
				});				
			}

			// delete
			if(self.user_info_.is_edit_allowed && (comment_info.created_by==self.user_info_.user_id))
			{
				var delete_container = $('<span></span>');
				delete_container.addClass('post-delete');

				var delete_ = $('<a>Delete</a>');
				delete_.attr('href','#');
				delete_.attr('title','Delete');

				delete_container.append(delete_);

				post_foot.append(delete_container);

				var dot = $('<span></span>');
				dot.addClass('dot');
				dot.html('&bull;');

				post_foot.append(dot);

				// delete events-apply
				delete_.on('click', function(e){
					e.preventDefault();
					self.buildDeleteConfirm_(comment_info.comment_id);
				});				
			}

			// reply 
			if(self.user_info_.is_add_allowed)
			{
				var reply_container = $('<span></span>');
				reply_container.addClass('post-reply');

				var reply = $('<a>Reply</a>');
				reply.attr('href','#');
				reply.attr('title', 'Reply');

				reply_container.append(reply);

				post_foot.append(reply_container);
			}

			post_container.append(post_foot);


			item.append(post_container);

			var clear = $('<div></div>');
			clear.addClass('clear');

			item.append(clear);

			var ul_child_elem = $('<ul></ul>');
			ul_child_elem.addClass('posted-comment-childs');
			ul_child_elem.attr('id', 'posted-comment-child-'+comment_info.comment_id);

			// postbox reply will be toggled show/hide by reply event
			if(self.user_info_.is_add_allowed)
			{
				var postbox = self.buildPostBox_(comment_info.comment_id);
				postbox.hide();
				ul_child_elem.append(postbox);

				// reply events-apply
				reply.on('click', function(e){
					e.preventDefault();
					postbox.toggle();
				});				

			}

			// check if has childrens
			if(comment_info.childrens.length>0)
			{
				for(var i=0;i<comment_info.childrens.length;i++)
				{
					var child = self.buildItemList_(comment_info.childrens[i]);
					ul_child_elem.append(child);
				}
			}

			item.append(ul_child_elem);

			return item[0];
		},

		buildCountList_: function( total_comment ) {
			var self = this;

			if(self.$total_comment===undefined)
			{
				self.$total_comment = $('<div></div>');
				self.$total_comment.addClass('comment-length');
			}
			
			self.$total_comment.html(self.total_comment+' '+self.options.title);

			return self.$total_comment;
		},

		removeItemList_: function( comment_id ){
			var self = this;

			// find target
			var target = $('#posted-'+comment_id, self.$elem);

			// remove target
			target.remove();
		},

		display_: function() {
			var self = this;

			self.$comment_display = $('<div></div>');
			self.$comment_display.addClass('comments-display');

			var tc = self.buildCountList_(self.total_comment);
			self.$comment_display.append(tc);

			// default comment post form reply
			var postbox = self.buildPostBox_(null);
			self.$comment_display.append(postbox);
			
			self.$rootlist = self.buildUl_();
			self.$rootlist.append(self.comments);

			self.$comment_display.append(self.$rootlist);


			if ( self.options.transition === 'none' || !self.options.transition ) {
				self.$elem.html( self.$comment_display );
			} else {
				self.$elem[ self.options.transition ]( 500, function() {
					self.$elem.html( self.$comment_display )[ self.options.transition ]( 500 );
				});
			}
		},

		timeStringToABBR_: function( time_string ) {
			var abbr_str = '';
			
			var split = time_string.split(' ');
			
			if(split.length==0)
				return abbr_str;

			abbr_str = split[0]+'T';

			if(split.length==2)
				abbr_str += split[1]+'Z';

			return abbr_str;
		},

		buildDeleteConfirm_: function( comment_id ) {
			var self = this;
			
			var delete_confirm = $('div[id="dialog-delete-comment-confirm"]');
			
			if(delete_confirm.length==0)
			{
				delete_confirm = $('<div></div>');
				delete_confirm.attr('id', 'dialog-delete-comment-confirm');
				delete_confirm.attr('title', 'Confirmation');

				var p = $('<p></p>');

				var icon_alert = $('<span class="ui-icon ui-icon-alert" style="float:left; margin:0 7px 50px 0;"></span>');

				var message = $('<span>Are You sure want to delete this data?</span>');

				p.append(icon_alert);
				p.append(message);
				delete_confirm.append(p);
				delete_confirm.hide().appendTo('body');
			}

		    return delete_confirm.dialog({
		        autoOpen: true,
		        modal: true,
		        buttons: {
		            Yes: function () {
		            	var form_data = { 'comment_id': comment_id };
						
						$.ajax({
							url: self.options.url_delete,
							data: form_data,
							type: 'post',
							dataType: 'json',
			            }).done( function(result){

			                if(result.success!=undefined)
			                {
			                    if(result.success===false)
			                    {
			                        // error
			                        $.each(result, function(key, val){
			                             // check error if any
			                            if(val.error!=undefined)
			                            {
			                                $show_warning_(val.error);
			                                return false;
			                            }
			                        });
			                    }
			                    else
			                    {
			                    	self.removeItemList_(comment_id);

			                    	self.total_comment = result.total_comment;
			                    	
			                    	self.$total_comment.html(self.total_comment+' '+self.options.title);
			                    	
			                    	delete_confirm.dialog("close");        	                    		
			                    }
			                }                   
			            });
		            },
		            No: function () {
		                delete_confirm.dialog("close");
		            }
		        }
		    });
		},

		limit_: function( obj, count ) {
			return obj.slice( 0, count );
		}
	};

	$.fn.comment = function( options ) {
		return this.each(function() {
			var comment = Object.create( Comment );
			
			comment.init_( options, this );

			$.data( this, 'comment', comment );
		});
	};

	// options
	$.fn.comment.options = {
		title: 'Notes',
		url_get: '#',
		url_input: '#',
		url_delete: '#',
		wrapEachWith: '<li></li>',
		limit: 10,
		auto_refresh: true,
		refresh: null,
		onComplete: null,
		transition: 'fadeToggle',
	};

})(jQuery, window, document);
