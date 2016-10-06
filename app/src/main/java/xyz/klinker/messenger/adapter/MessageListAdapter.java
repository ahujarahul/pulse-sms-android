/*
 * Copyright (C) 2016 Jacob Klinker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package xyz.klinker.messenger.adapter;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.annotation.VisibleForTesting;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.klinker.android.link_builder.Link;
import com.klinker.android.link_builder.LinkBuilder;
import com.klinker.android.link_builder.TouchableMovementMethod;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.activity.MessengerActivity;
import xyz.klinker.messenger.adapter.view_holder.MessageViewHolder;
import xyz.klinker.messenger.data.DataSource;
import xyz.klinker.messenger.data.MimeType;
import xyz.klinker.messenger.data.Settings;
import xyz.klinker.messenger.data.model.Contact;
import xyz.klinker.messenger.data.model.Conversation;
import xyz.klinker.messenger.data.model.Message;
import xyz.klinker.messenger.fragment.MessageListFragment;
import xyz.klinker.messenger.util.ColorUtils;
import xyz.klinker.messenger.util.DensityUtil;
import xyz.klinker.messenger.util.ImageUtils;
import xyz.klinker.messenger.util.PhoneNumberUtils;
import xyz.klinker.messenger.util.Regex;
import xyz.klinker.messenger.util.TimeUtils;
import xyz.klinker.messenger.util.listener.MessageDeletedListener;

/**
 * Adapter for displaying messages in a conversation.
 */
public class MessageListAdapter extends RecyclerView.Adapter<MessageViewHolder>
        implements MessageDeletedListener {

    private static final String TAG = "MessageListAdapter";

    private Cursor messages;
    private Map<String, Contact> fromColorMapper;
    private Map<String, Contact> fromColorMapperByName;
    private int receivedColor;
    private int accentColor;
    private boolean isGroup;
    private LinearLayoutManager manager;
    private MessageListFragment fragment;
    private int timestampHeight;

    public MessageListAdapter(Cursor messages, int receivedColor, int accentColor, boolean isGroup,
                              LinearLayoutManager manager, MessageListFragment fragment) {
        this.messages = messages;
        this.receivedColor = receivedColor;
        this.accentColor = accentColor;
        this.isGroup = isGroup;
        this.manager = manager;
        this.fragment = fragment;
        this.timestampHeight = 0;
    }

    @Override
    public MessageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        int layoutId;
        int color;

        if (timestampHeight == 0) {
            setTimestampHeight(DensityUtil.spToPx(parent.getContext(),
                    Settings.get(parent.getContext()).mediumFont + 2));
        }

        if (viewType == Message.TYPE_RECEIVED) {
            layoutId = R.layout.message_received;
            color = receivedColor;
        } else {
            color = -1;

            if (viewType == Message.TYPE_SENDING) {
                layoutId = R.layout.message_sending;
            } else if (viewType == Message.TYPE_ERROR) {
                layoutId = R.layout.message_error;
            } else if (viewType == Message.TYPE_DELIVERED) {
                layoutId = R.layout.message_delivered;
            } else if (viewType == Message.TYPE_INFO) {
                layoutId = R.layout.message_info;
            } else {
                layoutId = R.layout.message_sent;
            }
        }

        View view = LayoutInflater.from(parent.getContext())
                .inflate(layoutId, parent, false);

        messages.moveToFirst();
        return new MessageViewHolder(fragment, view, fromColorMapper != null && fromColorMapper.size() > 1 ? -1 : color,
                messages.getLong(messages.getColumnIndex(Message.COLUMN_CONVERSATION_ID)),
                viewType, timestampHeight, this);
    }

    @VisibleForTesting
    void setTimestampHeight(int height) {
        this.timestampHeight = height;
    }

    @Override
    public void onBindViewHolder(final MessageViewHolder holder, int position) {
        messages.moveToPosition(position);
        Message message = new Message();
        message.fillFromCursor(messages);

        holder.messageId = message.id;
        holder.mimeType = message.mimeType;

        colorMessage(holder, message);

        if (message.mimeType.equals(MimeType.TEXT_PLAIN)) {
            holder.message.setText(message.data);

            Link urls = new Link(Regex.WEB_URL);
            urls.setTextColor(accentColor);
            urls.setHighlightAlpha(.4f);
            urls.setOnClickListener(new Link.OnClickListener() {
                @Override
                public void onClick(String clickedText) {
                    if (!clickedText.startsWith("http")) {
                        clickedText = "http://" + clickedText;
                    }

                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(clickedText));
                    holder.message.getContext().startActivity(browserIntent);
                }
            });

            Link phoneNumbers = new Link(Regex.PHONE);
            phoneNumbers.setTextColor(accentColor);
            phoneNumbers.setHighlightAlpha(.4f);
            phoneNumbers.setOnClickListener(new Link.OnClickListener() {
                @Override
                public void onClick(String clickedText) {
                    Intent intent = new Intent(Intent.ACTION_DIAL);
                    intent.setData(Uri.parse("tel:" + PhoneNumberUtils.clearFormatting(clickedText)));
                    holder.message.getContext().startActivity(intent);
                }
            });

            if (holder.message.getMovementMethod() == null) {
                holder.message.setMovementMethod(new TouchableMovementMethod());
            }

            LinkBuilder.on(holder.message).addLink(urls).addLink(phoneNumbers).build();

            if (holder.image != null && holder.image.getVisibility() == View.VISIBLE) {
                holder.image.setVisibility(View.GONE);
            }

            if (holder.messageHolder != null && holder.messageHolder.getVisibility() == View.GONE) {
                holder.messageHolder.setVisibility(View.VISIBLE);
            }
        } else {
            holder.image.setImageDrawable(null);
            holder.image.setMinimumWidth(0);

            holder.image.setBackground(holder.image.getResources().getDrawable(R.drawable.rounded_rect));
            holder.image.setClipToOutline(true);

            if (MimeType.isStaticImage(message.mimeType)) {
                Glide.with(holder.image.getContext())
                        .load(Uri.parse(message.data))
                        .override(holder.image.getMaxHeight(), holder.image.getMaxHeight())
                        .into(holder.image);
            } else if (message.mimeType.equals(MimeType.IMAGE_GIF)) {
                holder.image.setMaxWidth(holder.image.getContext()
                        .getResources().getDimensionPixelSize(R.dimen.max_gif_width));
                Glide.with(holder.image.getContext())
                        .load(Uri.parse(message.data))
                        .fitCenter()
                        .into(holder.image);
            } else if (MimeType.isVideo(message.mimeType)) {
                Drawable placeholder;
                if (getItemViewType(position) != Message.TYPE_RECEIVED) {
                    placeholder = holder.image.getContext()
                            .getDrawable(R.drawable.ic_play_sent);
                } else {
                    placeholder = holder.image.getContext()
                            .getDrawable(R.drawable.ic_play);
                }

                Glide.with(holder.image.getContext())
                        .load(Uri.parse(message.data))
                        .asBitmap()
                        .error(placeholder)
                        .placeholder(placeholder)
                        .override(holder.image.getMaxHeight(), holder.image.getMaxHeight())
                        .into(new SimpleTarget<Bitmap>() {
                            @Override
                            public void onResourceReady(Bitmap resource,
                                                        GlideAnimation<? super Bitmap> glideAnimation) {
                                ImageUtils.overlayBitmap(holder.image.getContext(),
                                        resource, R.drawable.ic_play);
                                holder.image.setImageBitmap(resource);
                            }
                        });
            } else if (MimeType.isAudio(message.mimeType)) {
                Drawable placeholder;
                if (getItemViewType(position) != Message.TYPE_RECEIVED) {
                    placeholder = holder.image.getContext()
                            .getDrawable(R.drawable.ic_audio_sent);
                } else {
                    placeholder = holder.image.getContext()
                            .getDrawable(R.drawable.ic_audio);
                }

                Glide.with(holder.image.getContext())
                        .load(Uri.parse(message.data))
                        .error(placeholder)
                        .placeholder(placeholder)
                        .into(holder.image);
            } else if (MimeType.isVcard(message.mimeType)) {
                holder.message.setText(message.data);
                holder.image.setImageResource(getItemViewType(position) != Message.TYPE_RECEIVED ?
                        R.drawable.ic_contacts_sent : R.drawable.ic_contacts);
            } else {
                Log.v("MessageListAdapter", "unused mime type: " + message.mimeType);
            }


            if (holder.messageHolder.getVisibility() == View.VISIBLE) {
                holder.messageHolder.setVisibility(View.GONE);
            }

            if (holder.image.getVisibility() == View.GONE) {
                holder.image.setVisibility(View.VISIBLE);
            }
        }

        long nextTimestamp;
        if (position != getItemCount() - 1) {
            messages.moveToPosition(position + 1);
            nextTimestamp = messages.getLong(messages.getColumnIndex(Message.COLUMN_TIMESTAMP));
        } else {
            nextTimestamp = System.currentTimeMillis();
        }

        holder.timestamp.setText(TimeUtils.formatTimestamp(holder.timestamp.getContext(),
                message.timestamp));

        if (TimeUtils.shouldDisplayTimestamp(message.timestamp, nextTimestamp)) {
            holder.timestamp.getLayoutParams().height = timestampHeight;
        } else {
            holder.timestamp.getLayoutParams().height = 0;
        }

        if (isGroup && holder.contact != null && message.from != null) {
            if (holder.contact.getVisibility() == View.GONE) {
                holder.contact.setVisibility(View.VISIBLE);
            }

            int label = holder.timestamp.getLayoutParams().height > 0 ?
                    R.string.message_from_bullet : R.string.message_from;
            holder.contact.setText(holder.contact.getResources().getString(label, message.from));
        }
    }

    @Override
    public int getItemCount() {
        try {
            return messages.getCount();
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (messages == null || messages.getCount() == 0) {
            return -1;
        }

        messages.moveToPosition(position);
        return messages.getInt(messages.getColumnIndex(Message.COLUMN_TYPE));
    }

    public long getItemId(int position) {
        if (messages == null || messages.getCount() == 0) {
            return -1;
        }

        messages.moveToPosition(position);
        return messages.getLong(messages.getColumnIndex(Message.COLUMN_ID));
    }

    public void addMessage(Cursor newMessages) {
        int initialCount = getItemCount();

        messages = newMessages;

        if (newMessages != null) {
            int finalCount = getItemCount();

            if (initialCount == finalCount) {
                notifyItemChanged(finalCount - 1);
            } else if (initialCount > finalCount) {
                notifyDataSetChanged();
            } else {
                notifyItemInserted(finalCount - 1);
                manager.scrollToPosition(finalCount - 1);
            }
        }
    }

    public Cursor getMessages() {
        return messages;
    }

    @Override
    public void onMessageDeleted(Context context, long conversationId, int position) {
        if (position == getItemCount() - 1 && position != 0) {
            Log.v(TAG, "deleted last item, updating conversation");
            DataSource source = DataSource.getInstance(context);
            source.open();

            Message message = new Message();
            messages.moveToPosition(position - 1);
            message.fillFromCursor(messages);

            Conversation conversation = source.getConversation(conversationId);
            source.updateConversation(conversationId, conversation.read, message.timestamp,
                    message.type == Message.TYPE_SENT || message.type == Message.TYPE_SENDING ?
                            context.getString(R.string.you) + ": " + message.data : message.data,
                    message.mimeType, conversation.archive);

            source.close();
        } else if (position == 0 && (getItemCount() == 1 || getItemCount() == 0)) {
            ((MessengerActivity) fragment.getActivity()).menuItemClicked(R.id.menu_delete_conversation);
        } else {
            Log.v(TAG, "position not last, so leaving conversation");
        }
    }

    public void setFromColorMapper(Map<String, Contact> colorMapper, Map<String, Contact> colorMapperByName) {
        this.fromColorMapper = colorMapper;
        this.fromColorMapperByName = colorMapperByName;
    }

    private void colorMessage(final MessageViewHolder holder, final Message message) {
        if (message.type == Message.TYPE_RECEIVED &&
                fromColorMapper != null && fromColorMapper.size() > 1) { // size > 1 so we know it is a group convo
            if (fromColorMapper.containsKey(message.from)) {
                // group convo, color them differently
                // this is the usual result
                holder.messageHolder.setBackgroundTintList(
                        ColorStateList.valueOf(fromColorMapper.get(message.from).colors.color));
            } else if (fromColorMapperByName != null && fromColorMapperByName.containsKey(message.from)) {
                // group convo, color them differently
                // this is the usual result
                holder.messageHolder.setBackgroundTintList(
                        ColorStateList.valueOf(fromColorMapperByName.get(message.from).colors.color));
            } else {
                // group convo without the contact here.. uh oh. Could happen if the conversation
                // title doesn't match the message.from database column.
                final Contact contact = new Contact();
                contact.name = message.from;
                contact.phoneNumber = message.from;
                contact.colors = ColorUtils.getRandomMaterialColor(holder.itemView.getContext());

                if (fromColorMapper == null) {
                    fromColorMapper = new HashMap<>();
                }

                fromColorMapper.put(message.from, contact);

                // then write it to the database for later
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        DataSource source = DataSource.getInstance(holder.itemView.getContext());
                        source.open();
                        source.insertContact(contact);
                        source.close();
                    }
                }).start();
            }
        }
    }

}
